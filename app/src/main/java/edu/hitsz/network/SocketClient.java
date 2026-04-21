package edu.hitsz.network;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Socket客户端 - 管理与服务器的连接和消息收发
 *
 * 使用方式:
 *   1. SocketClient.getInstance() 获取单例
 *   2. setConnectionListener() 设置连接状态回调
 *   3. setMessageListener() 设置消息接收回调
 *   4. connect(ip, port) 连接服务器
 *   5. sendMessage() / joinRoom() / sendScoreUpdate() / sendGameOver() 发送消息
 */
public class SocketClient {

    private static final String TAG = "SocketClient";

    /** 默认服务器地址（测试用，实际使用时由用户输入） */
    private static final String DEFAULT_SERVER_IP = "192.168.1.100";
    private static final int DEFAULT_SERVER_PORT = 8080;

    // 单例模式
    private static volatile SocketClient instance;

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private boolean isConnected = false;

    // 线程池：用于网络IO操作（不能在主线程做网络操作）
    private final ExecutorService executor = Executors.newCachedThreadPool();;

    // 主线程Handler：用于把网络结果回调到UI线程
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // 监听器接口

    public interface OnMessageReceivedListener {
        /** 收到消息时回调（已在UI线程）*/
        void onMessageReceived(String messageType, String body);
        /** 连接断开时回调（已在UI线程）*/
        void onDisconnected();
    }

    public interface OnConnectionStateChangedListener {
        /** 连接成功回调（已在UI线程）*/
        void onConnected();
        /** 连接断开/失败回调（已在UI线程）*/
        void onDisconnected(String reason);
    }

    private OnMessageReceivedListener messageListener;
    private OnConnectionStateChangedListener connectionListener;

    // 单例获取

    public static SocketClient getInstance() {
        if (instance == null) {
            synchronized (SocketClient.class) {
                if (instance == null) {
                    instance = new SocketClient();
                }
            }
        }
        return instance;
    }

    private SocketClient() { }  // 私有构造

    // 连接管理

    /**
     * 连接到指定服务器
     * @param serverIp 服务器IP地址
     * @param port     服务器端口
     */
    public void connect(final String serverIp, final int port) {
        executor.execute(() -> {
            try {
                Log.d(TAG, "正在连接: " + serverIp + ":" + port);

                socket = new Socket();
                socket.connect(new InetSocketAddress(serverIp, port), 8000); // 8秒连接超时

                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new PrintWriter(socket.getOutputStream(), true);
                isConnected = true;

                Log.d(TAG, "连接成功!");

                // 回调到UI线程: 连接成功
                mainHandler.post(() -> {
                    if (connectionListener != null) {
                        connectionListener.onConnected();
                    }
                });

                // 连接成功后开始循环监听消息
                startListening();

            } catch (IOException e) {
                Log.e(TAG, "连接失败: " + e.getMessage());
                isConnected = false;

                // 回调到UI线程: 连接失败
                mainHandler.post(() -> {
                    if (connectionListener != null) {
                        connectionListener.onDisconnected("连接失败: " + e.getMessage());
                    }
                });
            }
        });
    }

    /**
     * 使用默认地址连接
     */
    public void connect() {
        connect(DEFAULT_SERVER_IP, DEFAULT_SERVER_PORT);
    }

    // 消息监听

    /**
     * 启动消息监听循环（在子线程中阻塞式读取）
     * 注意：此方法必须在connect成功后调用！
     */
    private void startListening() {
        executor.execute(() -> {
            try {
                String line;
                while (isConnected && (line = reader.readLine()) != null) {
                    Log.d(TAG, "收到消息: " + line);

                    // 解析消息格式: TYPE|body
                    String[] parts = line.split("\\|", 2);
                    if (parts.length >= 2) {
                        final String type = parts[0];
                        final String body = parts[1];

                        // 切换到UI线程回调
                        mainHandler.post(() -> {
                            if (messageListener != null) {
                                messageListener.onMessageReceived(type, body);
                            }
                        });
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "消息监听异常: " + e.getMessage());
            } finally {
                // 循环退出 = 连接断开
                isConnected = false;
                mainHandler.post(() -> {
                    if (messageListener != null) {
                        messageListener.onDisconnected();
                    }
                    if (connectionListener != null) {
                        connectionListener.onDisconnected("连接已断开");
                    }
                });
            }
        });
    }

    // 发送消息方法

    /**
     * 发送通用消息
     */
    public void sendMessage(final String type, final String body) {
        executor.execute(() -> {
            if (writer != null && isConnected) {
                String msg = type + "|" + body;
                Log.d(TAG, "发送: " + msg);
                writer.println(msg);
                if (writer.checkError()) {
                    Log.e(TAG, "发送失败!");
                }
            } else {
                Log.w(TAG, "发送失败: 未连接");
            }
        });
    }

    /**
     * 加入房间（发送昵称）
     */
    public void joinRoom(String playerName) {
        sendMessage("JOIN_ROOM", playerName);
    }

    /**
     * 发送分数更新
     */
    public void sendScoreUpdate(int score) {
        sendMessage("SCORE_UPDATE", String.valueOf(score));
    }

    /**
     * 发送游戏结束（我死了）
     */
    public void sendGameOver(int finalScore) {
        sendMessage("GAME_OVER", String.valueOf(finalScore));
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        executor.execute(() -> {
            isConnected = false;
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "关闭连接异常: " + e.getMessage());
            }
        });
    }

    // 状态查询

    /** 是否已连接 */
    public boolean connected() {
        return isConnected;
    }

    // 设置监听器

    public void setMessageListener(OnMessageReceivedListener listener) {
        this.messageListener = listener;
    }

    public void setConnectionListener(OnConnectionStateChangedListener listener) {
        this.connectionListener = listener;
    }
}

