package edu.hitsz.network;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import edu.hitsz.R;

/**
 * 联机对战大厅
 * 功能: 输入服务器IP、端口、昵称，然后连接服务器并匹配对手
 */
public class OnlineLobbyActivity extends AppCompatActivity {

    private EditText etServerIp;      // 服务器IP输入框
    private EditText etPort;          // 端口输入框
    private EditText etPlayerName;    // 昵称输入框
    private Button btnConnect;        // 匹配按钮
    private TextView tvStatus;        // 状态文本
    private Button btnBack;           // 返回按钮

    private SocketClient socketClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online_lobby);

        // 绑定控件
        etServerIp = findViewById(R.id.et_server_ip);
        etPort = findViewById(R.id.et_port);
        etPlayerName = findViewById(R.id.et_player_name);
        btnConnect = findViewById(R.id.btn_connect);
        tvStatus = findViewById(R.id.tv_status);
        btnBack = findViewById(R.id.btn_back);
        Intent intent = getIntent();
        String passedMusicMode = intent.getStringExtra("musicMode");
        String passedUsername = intent.getStringExtra("username");

        // 填充默认值
        etServerIp.setText("10.250.88.235");      // Android模拟器访问本机的地址
        etPort.setText("8080");
        etPlayerName.setText(passedUsername != null && !passedUsername.isEmpty()
                ? passedUsername : "Player");

        // 获取Socket客户端单例
        socketClient = SocketClient.getInstance();

        // 点击事件绑定

        // 匹配按钮点击
        btnConnect.setOnClickListener(v -> attemptConnect());

        // 返回按钮点击
        btnBack.setOnClickListener(v -> finish());
    }

    /**
     * 尝试连接服务器
     */
    private void attemptConnect() {
        // 读取用户输入
        String ip = etServerIp.getText().toString().trim();
        String portStr = etPort.getText().toString().trim();
        String name = etPlayerName.getText().toString().trim();

        // 校验输入
        if (ip.isEmpty()) {
            Toast.makeText(this, "请输入服务器IP", Toast.LENGTH_SHORT).show();
            return;
        }
        if (portStr.isEmpty()) {
            Toast.makeText(this, "请输入端口号", Toast.LENGTH_SHORT).show();
            return;
        }
        if (name.isEmpty()) {
            Toast.makeText(this, "请输入昵称", Toast.LENGTH_SHORT).show();
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "端口号必须是数字", Toast.LENGTH_SHORT).show();
            return;
        }

        // 更新UI状态
        tvStatus.setText("正在连接服务器...");
        tvStatus.setTextColor(Color.YELLOW);  // 黄色
        btnConnect.setEnabled(false);         // 禁用按钮防止重复点击
        btnConnect.setText("连接中...");

        // 设置监听器
        setupListeners(name);

        // 发起连接（异步）
        socketClient.connect(ip, port);
    }

    /**
     * 设置连接状态和消息的监听器
     * @param playerName 当前玩家昵称
     */
    private void setupListeners(final String playerName) {

        // 连接状态监听
        socketClient.setConnectionListener(new SocketClient.OnConnectionStateChangedListener() {
            @Override
            public void onConnected() {
                tvStatus.setText("[OK] 已连接到服务器!\n正在匹配对手...");
                tvStatus.setTextColor(Color.GREEN);

                // 连接成功后立即发送加入房间请求
                socketClient.joinRoom(playerName);
            }

            @Override
            public void onDisconnected(final String reason) {
                tvStatus.setText("[FAIL] " + reason);
                tvStatus.setTextColor(Color.RED);
                btnConnect.setEnabled(true);
                btnConnect.setText("重新连接");
            }
        });

        // 消息接收监听
        socketClient.setMessageListener(new SocketClient.OnMessageReceivedListener() {
            @Override
            public void onMessageReceived(final String type, final String body) {
                switch (type) {

                    case "WAITING_OPPONENT":
                        // 还在等待对手加入
                        tvStatus.setText("[WAIT] " + body);
                        tvStatus.setTextColor(Color.YELLOW);
                        break;

                    case "OPPONENT_JOINED":
                        // 对手找到了! 准备进入游戏
                        tvStatus.setText("[MATCH] 对手 [" + body + "] 已就绪!\n即将进入对局...");
                        tvStatus.setTextColor(Color.CYAN);

                        // 延迟1.5秒后跳转到游戏界面
                        tvStatus.postDelayed(() -> goToGame(playerName, body), 1500);
                        break;

                    case "ROOM_FULL":
                        tvStatus.setText("[WARN] 房间已满，请稍后再试");
                        tvStatus.setTextColor(Color.RED);
                        btnConnect.setEnabled(true);
                        btnConnect.setText("开始匹配");
                        break;

                    case "ERROR":
                        tvStatus.setText("[ERR] 服务器错误: " + body);
                        tvStatus.setTextColor(Color.RED);
                        btnConnect.setEnabled(true);
                        btnConnect.setText("开始匹配");
                        break;

                    default:
                        Log.d("Lobby", "收到未处理消息: " + type + "|" + body);
                }
            }

            @Override
            public void onDisconnected() {
                runOnUiThread(() -> {
                    tvStatus.setText("[FAIL] 与服务器失去连接");
                    tvStatus.setTextColor(Color.RED);
                    btnConnect.setEnabled(true);
                    btnConnect.setText("重新连接");
                });
            }
        });
    }

    /**
     * 跳转到在线游戏界面
     */
    private void goToGame(String myName, String opponentName) {
        Intent intent = new Intent(OnlineLobbyActivity.this, GameOnlineActivity.class);
        intent.putExtra("playerName", myName);
        intent.putExtra("opponentName", opponentName);

        // 新增：传递音乐设置和原始用户名
        Intent srcIntent = getIntent();
        intent.putExtra("musicMode", srcIntent.getStringExtra("musicMode"));
        intent.putExtra("username", srcIntent.getStringExtra("username"));

        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 不在这里断开连接，由GameOnline管理生命周期
    }
}
