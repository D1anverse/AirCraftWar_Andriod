package edu.hitsz.application;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import edu.hitsz.R;
import edu.hitsz.dao.PlayerDataManager;
import edu.hitsz.entity.PlayerData;
import edu.hitsz.network.SocketClient;

/**
 * 登录/注册页面
 *
 * 功能：
 *   - 切换登录/注册模式
 *   - 联网时：通过 Socket 发送 LOGIN / REGISTER 给服务器验证
 *   - 离线时：点击"跳过"直接进入主菜单
 */
public class LoginActivity extends AppCompatActivity {

    private EditText etNickname;
    private EditText etEmail;
    private EditText etPassword;
    private Button btnSubmit;
    private TextView tvSwitchMode;
    private Button btnSkip;

    private boolean isRegisterMode = false; // true=注册, false=登录

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 如果已经登录过，确保连接正常后再进主页
        if (UserSessionManager.getInstance(this).isLoggedIn()) {
            ensureConnectionAndStartMain();
            return;
        }

        initViews();
        setupListeners();
    }

    /**
     * 确保 Socket 连接正常后再进入主菜单
     */
    private void ensureConnectionAndStartMain() {
        SocketClient client = SocketClient.getInstance();

        // ensureConnected 内部会先重置连接再尝试连接
        client.ensureConnected(success -> {
            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(this, "已连接到服务器", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "服务器连接失败，将以离线模式运行", Toast.LENGTH_SHORT).show();
                }
                startMainMenu();
            });
        });
    }

    private void initViews() {
        etNickname = findViewById(R.id.et_nickname);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnSubmit = findViewById(R.id.btn_submit);
        tvSwitchMode = findViewById(R.id.tv_switch_mode);
        btnSkip = findViewById(R.id.btn_skip);
    }

    private void setupListeners() {
        // 初始状态：根据模式设置邮箱可见性
        etEmail.setVisibility(isRegisterMode ? View.VISIBLE : View.GONE);

        // 切换登录/注册模式
        tvSwitchMode.setOnClickListener(v -> {
            isRegisterMode = !isRegisterMode;
            if (isRegisterMode) {
                tvSwitchMode.setText("已有账号？点击登录");
                btnSubmit.setText("注 册");
                etEmail.setVisibility(View.VISIBLE); // 注册需要邮箱
            } else {
                tvSwitchMode.setText("还没有账号？点击注册");
                btnSubmit.setText("登 录");
                etEmail.setVisibility(View.GONE); // 登录不需要显示邮箱
            }
        });

        // 提交按钮（登录或注册）
        btnSubmit.setOnClickListener(v -> attemptAuth());

        // 跳过按钮（离线模式）
        btnSkip.setOnClickListener(v -> {
            Toast.makeText(this, "离线模式：部分功能不可用", Toast.LENGTH_SHORT).show();
            startMainMenu();
        });
    }

    /**
     * 尝试登录/注册
     * 协议格式：
     *   登录 → 发送 LOGIN|nickname,password
     *   注册 → 发送 REGISTER|nickname,email,password
     */
    private void attemptAuth() {
        String nickname = etNickname.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (nickname.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "请填写昵称和密码", Toast.LENGTH_SHORT).show();
            return;
        }

        SocketClient socketClient = SocketClient.getInstance();

        // 检查是否已连接
        if (!socketClient.connected()) {
            // 未连接，先尝试连接，连接成功后再执行认证
            Toast.makeText(this, "正在连接服务器...", Toast.LENGTH_SHORT).show();
            socketClient.setConnectionListener(new SocketClient.OnConnectionStateChangedListener() {
                @Override
                public void onConnected() {
                    runOnUiThread(() -> {
                        socketClient.setConnectionListener(null);
                        doAuth(socketClient);
                    });
                }

                @Override
                public void onDisconnected(String reason) {
                    runOnUiThread(() -> {
                        socketClient.setConnectionListener(null);
                        Toast.makeText(LoginActivity.this, "连接服务器失败：" + reason, Toast.LENGTH_LONG).show();
                    });
                }
            });
            socketClient.connect();
            return;
        }

        doAuth(socketClient);
    }

    /**
     * 执行实际的登录/注册逻辑
     */
    private void doAuth(SocketClient socketClient) {
        if (isRegisterMode) {
            // 注册模式
            String email = etEmail.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(this, "请填写邮箱", Toast.LENGTH_SHORT).show();
                return;
            }
            // 发送注册请求：REGISTER|nickname,email,password
            socketClient.sendMessage("REGISTER", etNickname.getText().toString().trim() + "," + email + "," + etPassword.getText().toString().trim());
        } else {
            // 登录模式：发送 LOGIN|nickname,password
            socketClient.sendMessage("LOGIN", etNickname.getText().toString().trim() + "," + etPassword.getText().toString().trim());
        }

        // 设置消息监听，等待服务器响应
        socketClient.setMessageListener(new SocketClient.OnMessageReceivedListener() {
            @Override
            public void onMessageReceived(String messageType, String body) {
                handleAuthResponse(messageType, body);
            }

            @Override
            public void onDisconnected() {
                runOnUiThread(() ->
                        Toast.makeText(LoginActivity.this, "连接断开", Toast.LENGTH_SHORT).show());
            }
        });

        Toast.makeText(this, isRegisterMode ? "正在注册..." : "正在登录...", Toast.LENGTH_SHORT).show();
    }

    /**
     * 处理服务器认证响应
     * 期望的服务器返回格式：
     *   成功 → LOGIN_SUCCESS|userId,nickname,email
     *   或   REGISTER_SUCCESS|userId,nickname,email
     *   失败 → LOGIN_ERROR|错误原因
     *   或   REGISTER_ERROR|错误原因
     */
    private void handleAuthResponse(String type, String body) {
        runOnUiThread(() -> {
            switch (type) {
                case "LOGIN_SUCCESS":
                case "REGISTER_SUCCESS": {
                    // 解析返回的用户信息（服务器返回 6 个字段：userId,nickname,email,coins,pro,promax）
                    String[] parts = body.split(",");
                    if (parts.length >= 3) {
                        int userId = Integer.parseInt(parts[0]);
                        String nickname = parts[1];
                        String email = parts[2];

                        // 保存登录状态
                        UserSessionManager session = UserSessionManager.getInstance(LoginActivity.this);
                        session.saveLoginInfo(userId, nickname, email);

                        // ★ 解析服务器返回的代币和战机解锁状态，并写入本地 SQLite
                        if (parts.length >= 4) {
                            int serverCoins = Integer.parseInt(parts[3]);
                            session.saveServerCoins(serverCoins);

                            PlayerDataManager localDB = new PlayerDataManager(LoginActivity.this);
                            PlayerData playerData = localDB.getOrCreatePlayerData(userId);
                            playerData.setCoins(serverCoins);
                            if (parts.length >= 5) {
                                playerData.setUnlockedPro("1".equals(parts[4]));
                            }
                            if (parts.length >= 6) {
                                playerData.setUnlockedPromax("1".equals(parts[5]));
                            }
                            localDB.insertOrUpdate(playerData);
                        }

                        Toast.makeText(this, "欢迎回来，" + nickname + "！", Toast.LENGTH_SHORT).show();

                        // 登录成功后，同步本地缓存的代币
                        syncPendingCoins(userId);

                        startMainMenu();
                    }
                    break;
                }
                case "LOGIN_ERROR":
                    Toast.makeText(this, "登录失败：" + body, Toast.LENGTH_SHORT).show();
                    break;
                case "REGISTER_ERROR":
                    Toast.makeText(this, "注册失败：" + body, Toast.LENGTH_SHORT).show();
                    break;
                default:
                    // 其他消息类型忽略（比如 SCORE_UPDATE 等）
                    break;
            }
        });
    }

    /**
     * 同步本地缓存的代币到服务器
     */
    private void syncPendingCoins(int userId) {
        UserSessionManager session = UserSessionManager.getInstance(this);
        int pendingCoins = session.getPendingCoins();

        if (pendingCoins <= 0) {
            return;
        }

        SocketClient client = SocketClient.getInstance();
        if (!client.connected()) {
            return;
        }

        client.sendMessage("SYNC_COINS", userId + "," + pendingCoins);

        client.setMessageListener(new SocketClient.OnMessageReceivedListener() {
            @Override
            public void onMessageReceived(String msgType, String msgBody) {
                if ("SYNC_OK".equals(msgType)) {
                    runOnUiThread(() -> {
                        session.saveServerCoins(session.getCachedCoins() + pendingCoins);
                        session.clearPendingCoins();
                        Toast.makeText(LoginActivity.this,
                                "已同步 " + pendingCoins + " 缓存代币！", Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onDisconnected() {}
        });
    }

    private void startMainMenu() {
        Intent intent = new Intent(this, MenuActivity.class);
        startActivity(intent);
        finish();
    }
}
