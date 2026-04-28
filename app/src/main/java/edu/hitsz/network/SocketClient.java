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
    private static final String DEFAULT_SERVER_IP = "10.250.88.235";
    private static final int DEFAULT_SERVER_PORT = 8080;

    // 单例模式
    private static volatile SocketClient instance;

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private volatile boolean isConnected = false;

    // 保存连接时的服务器地址
    private String lastConnectedIp;
    private int lastConnectedPort;

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

    /** 一次性的查询回调（用于 ShopActivity 等临时查询） */
    private OnMessageReceivedListener pendingQueryCallback;

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

                // ★ 修复1: 先关闭旧连接，防止重复连接导致服务端残留旧handler
                resetConnection();

                socket = new Socket();
                socket.connect(new InetSocketAddress(serverIp, port), 8000); // 8秒连接超时

                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new PrintWriter(socket.getOutputStream(), true);
                isConnected = true;

                // 保存连接地址（用于重连）
                lastConnectedIp = serverIp;
                lastConnectedPort = port;

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
                            // 优先派发给一次性查询回调（用于临时查询如 QUERY_COINS）
                            if (pendingQueryCallback != null) {
                                pendingQueryCallback.onMessageReceived(type, body);
                            } else if (messageListener != null) {
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
     * 用户登录
     * 发送：LOGIN|nickname,password
     * 期望返回：LOGIN_SUCCESS|userId,nickname,email  或  LOGIN_ERROR|reason
     */
    public void sendLogin(String nickname, String password) {
        sendMessage("LOGIN", nickname + "," + password);
    }

    /**
     * 用户注册
     * 发送：REGISTER|nickname,email,password
     * 期望返回：REGISTER_SUCCESS|userId,nickname,email  或  REGISTER_ERROR|reason
     */
    public void sendRegister(String nickname, String email, String password) {
        sendMessage("REGISTER", nickname + "," + email + "," + password);
    }

    /**
     * 搜索用户
     * 发送：SEARCH_USER|keyword
     * 期望返回：SEARCH_RESULT|userId,nickname,status
     */
    public void sendSearchUser(String keyword) {
        sendMessage("SEARCH_USER", keyword);
    }

    /**
     * 添加好友
     * 发送：ADD_FRIEND|myUserId,friendId
     * 期望返回：ADD_FRIEND_OK  或  ADD_FRIEND_FAIL|reason
     */
    public void sendAddFriend(int myUserId, int targetUserId) {
        sendMessage("ADD_FRIEND", myUserId + "," + targetUserId);
    }

    /**
     * 获取好友列表
     * 发送：GET_FRIENDS|userId
     * 期望返回：FRIEND_LIST|数据（多条用换行分隔）
     */
    public void sendGetFriends(int userId) {
        sendMessage("GET_FRIENDS", String.valueOf(userId));
    }

    /**
     * 邀请好友对战
     * 发送：INVITE_FRIEND|fromUserId,toUserId
     * 期望返回：INVITE_SENT  或  INVITE_FAIL|reason
     */
    public void sendInviteFriend(int fromUserId, int toUserId) {
        sendMessage("INVITE_FRIEND", fromUserId + "," + toUserId);
    }

    /**
     * 接受/拒绝邀请
     * 发送：RESPOND_INVITE|toUserId,ACCEPT/REJECT
     */
    public void sendRespondInvite(int toUserId, boolean accept) {
        sendMessage("RESPOND_INVITE", toUserId + "," + (accept ? "ACCEPT" : "REJECT"));
    }

    /**
     * 商店购买同步
     * 发送：BUY_AIRCRAFT|userId,aircraftType(PRO/PROMAX)
     * 期望返回：BUY_OK  或  BUY_FAIL|reason
     */
    public void sendBuyAircraft(int userId, String aircraftType) {
        sendMessage("BUY_AIRCRAFT", userId + "," + aircraftType);
    }

    /**
     * 上报代币变动（游戏结束/其他场景）
     * 发送：SYNC_COINS|userId,coinAmount
     */
    public void sendSyncCoins(int userId, int coinAmount) {
        sendMessage("SYNC_COINS", userId + "," + coinAmount);
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

    /**
     * 重置连接状态（用于应用重启时强制重连）
     * 同步执行，确保立即断开旧连接
     */
    public void resetConnection() {
        synchronized (this) {
            isConnected = false;
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "关闭连接异常: " + e.getMessage());
            }
            socket = null;
            reader = null;
            writer = null;
            Log.d(TAG, "连接状态已重置");
        }
    }

    /**
     * 确保连接正常（如果断开则尝试重连）
     * @param callback 连接状态回调
     */
    public void ensureConnected(ConnectionCallback callback) {
        // 如果已经连接，直接回调成功
        if (isConnected && socket != null && !socket.isClosed()) {
            Log.d(TAG, "当前已连接，直接返回成功");
            callback.onResult(true);
            return;
        }

        // 保存原有的监听器
        final OnConnectionStateChangedListener originalListener = connectionListener;
        final OnMessageReceivedListener originalMessageListener = messageListener;

        // 先关闭旧连接
        resetConnection();

        // 延迟一点再尝试连接，确保旧连接已完全关闭
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            String ip = (lastConnectedIp != null) ? lastConnectedIp : DEFAULT_SERVER_IP;
            int port = (lastConnectedPort > 0) ? lastConnectedPort : DEFAULT_SERVER_PORT;

            Log.d(TAG, "尝试连接到: " + ip + ":" + port);

            // 设置临时回调用于重连
            connectionListener = new OnConnectionStateChangedListener() {
                @Override
                public void onConnected() {
                    Log.d(TAG, "重连成功!");
                    // 恢复原有的监听器
                    connectionListener = originalListener;
                    messageListener = originalMessageListener;
                    // 调用原有监听器
                    if (originalListener != null) {
                        originalListener.onConnected();
                    }
                    callback.onResult(true);
                }

                @Override
                public void onDisconnected(String reason) {
                    Log.e(TAG, "重连失败: " + reason);
                    // 恢复原有的监听器
                    connectionListener = originalListener;
                    callback.onResult(false);
                }
            };

            connect(ip, port);
        }, 300);
    }

    /**
     * 连接状态回调接口
     */
    public interface ConnectionCallback {
        void onResult(boolean success);
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

    /**
     * 设置一次性查询回调（查询完成后自动清除，不会覆盖 setMessageListener）
     * 常用于 ShopActivity 等临时向服务器请求数据
     */
    public void setQueryCallback(OnMessageReceivedListener callback) {
        this.pendingQueryCallback = callback;
    }

    /** 清除一次性查询回调 */
    public void clearQueryCallback() {
        this.pendingQueryCallback = null;
    }

    /**
     * 查询玩家代币数据
     * 发送：QUERY_COINS|userId
     * 期望返回：COINS_INFO|userId,coins,proUnlocked,promaxUnlocked
     */
    public void sendQueryCoins(int userId) {
        sendMessage("QUERY_COINS", String.valueOf(userId));
    }

    /**
     * 发送难度选择（联机对战）
     * 发送：DIFFICULTY_SELECT|easy/normal/hard
     */
    public void sendDifficultySelect(String difficulty) {
        sendMessage("DIFFICULTY_SELECT", difficulty);
    }

    /**
     * 发送飞机选择（联机对战）
     * 发送：AIRCRAFT_SELECT|heroTypeId
     */
    public void sendAircraftSelect(String heroTypeId) {
        sendMessage("AIRCRAFT_SELECT", heroTypeId);
    }

    /**
     * 发送确认完成（联机对战）
     * 发送：PLAYER_READY
     */
    public void sendPlayerReady() {
        sendMessage("PLAYER_READY", "");
    }

    /**
     * 离开房间
     * 发送：LEAVE_ROOM
     */
    public void leaveRoom() {
        sendMessage("LEAVE_ROOM", "");
    }
}

