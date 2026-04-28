package edu.hitsz.application;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import edu.hitsz.application.GameConfig;
import edu.hitsz.application.ImageManager;
import edu.hitsz.R;
import edu.hitsz.dao.PlayerDataManager;
import edu.hitsz.entity.PlayerData;
import edu.hitsz.network.OnlineLobbyActivity;
import edu.hitsz.network.SocketClient;

public class MenuActivity extends AppCompatActivity {

    private EditText etUsername;
    private RadioGroup radioGroup;
    private CheckBox cbMusic;
    private Button btnStart, btnRank, btnShop, btnFriends, btnOnline;
    private Button btnLogout, btnLogin;  // 两个状态按钮
    private TextView tvUserNickname, tvUserCoins;

    private PlayerDataManager playerDataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        GameConfig.init(this);
        ImageManager.init(getResources());
        playerDataManager = new PlayerDataManager(this);

        initViews();
        loadUserInfo();
        setupListeners();
    }

    private void initViews() {
        etUsername = findViewById(R.id.et_username);
        radioGroup = findViewById(R.id.radio_group_difficulty);
        cbMusic = findViewById(R.id.cb_music);
        btnStart = findViewById(R.id.btn_start);
        btnRank = findViewById(R.id.btn_rank);
        btnShop = findViewById(R.id.btn_shop);
        btnFriends = findViewById(R.id.btn_friends);
        btnOnline = findViewById(R.id.btn_online);

        // ★ 改动1：两个按钮都绑定
        btnLogout = findViewById(R.id.btn_logout);
        btnLogin = findViewById(R.id.btn_login);

        tvUserNickname = findViewById(R.id.tv_user_nickname);
        tvUserCoins = findViewById(R.id.tv_user_coins);

        etUsername.setText("Player");
    }

    /**
     * 加载用户信息 + 根据登录状态切换UI
     */
    private void loadUserInfo() {
        UserSessionManager session = UserSessionManager.getInstance(this);

        if (session.isLoggedIn()) {
            // ===== 已登录 =====
            tvUserNickname.setText(session.getNickname());

            int userId = session.getUserId();
            if (userId > 0) {
                PlayerData data = playerDataManager.getOrCreatePlayerData(userId);
                tvUserCoins.setText("💰 " + data.getCoins());
                tvUserCoins.setVisibility(View.VISIBLE);
            }

            // 显示"退出登录"，隐藏"去登录"
            btnLogout.setVisibility(View.VISIBLE);
            btnLogin.setVisibility(View.GONE);

            // 启用所有联网功能按钮
            setOnlineButtonsEnabled(true);

        } else {
            // ===== 游客/离线模式 =====
            tvUserNickname.setText("🔓 游客（离线模式）");
            tvUserCoins.setVisibility(View.GONE);

            // 显示"去登录"，隐藏"退出登录"
            btnLogout.setVisibility(View.GONE);
            btnLogin.setVisibility(View.VISIBLE);

            // 禁用所有需要联网的功能
            setOnlineButtonsEnabled(false);
        }
    }

    /**
     * 统一设置联网相关按钮的启用/禁用状态
     * @param enabled true=全部可用, false=禁用并变灰
     */
    private void setOnlineButtonsEnabled(boolean enabled) {
        float alpha = enabled ? 1.0f : 0.5f;

        // 联机对战 — 必须登录才能用
        btnOnline.setEnabled(enabled);
        btnOnline.setAlpha(alpha);

        // 好友 — 必须登录才能用
        btnFriends.setEnabled(enabled);
        btnFriends.setAlpha(alpha);

        // 商店 — 必须登录才能用（代币是账户数据）
        btnShop.setEnabled(enabled);
        btnShop.setAlpha(alpha);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshCoinDisplay();
        loadUserInfo();
        // 已登录用户重连后通知服务器上线
        notifyOnlineStatus();
    }

    /**
     * 已登录用户重连后通知服务器上线
     */
    private void notifyOnlineStatus() {
        UserSessionManager session = UserSessionManager.getInstance(this);
        if (!session.isLoggedIn()) {
            return;
        }

        SocketClient client = SocketClient.getInstance();
        if (!client.connected()) {
            // 未连接，先重连
            client.ensureConnected(success -> {
                if (success) {
                    runOnUiThread(() -> {
                        // 发送JOIN_ROOM时带上userId，格式：userId,nickname
                        client.sendMessage("JOIN_ROOM", session.getUserId() + "," + session.getNickname());
                    });
                }
            });
        } else {
            // 已连接，发送上线通知（带上userId）
            client.sendMessage("JOIN_ROOM", session.getUserId() + "," + session.getNickname());
        }
    }

    private void refreshCoinDisplay() {
        UserSessionManager session = UserSessionManager.getInstance(this);
        if (session.isLoggedIn() && session.getUserId() > 0) {
            PlayerData data = playerDataManager.getOrCreatePlayerData(session.getUserId());
            tvUserCoins.setText("💰 " + data.getCoins());
            tvUserCoins.setVisibility(View.VISIBLE);
        }
    }

    private void setupListeners() {
        // 开始游戏（单机模式）— 不需要登录
        btnStart.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            if (username.isEmpty()) {
                Toast.makeText(this, "请输入用户名", Toast.LENGTH_SHORT).show();
                return;
            }
            int selectedId = radioGroup.getCheckedRadioButtonId();
            String difficulty;
            if (selectedId == R.id.radio_easy) difficulty = "easy";
            else if (selectedId == R.id.radio_normal) difficulty = "normal";
            else difficulty = "hard";

            boolean musicOn = cbMusic.isChecked();
            String musicMode = musicOn ? "ON" : "OFF";

            Intent intent = new Intent(this, HeroSelectActivity.class);
            intent.putExtra("username", username);
            intent.putExtra("difficulty", difficulty);
            intent.putExtra("musicMode", musicMode);
            startActivity(intent);
        });

        // 排行榜 — 不需要登录（本地排行榜）
        btnRank.setOnClickListener(v -> {
            startActivity(new Intent(this, ScoreActivity.class));
        });

        // 商店 — 需要登录
        btnShop.setOnClickListener(v -> {
            if (!checkLogin()) return;
            Intent intent = new Intent(this, ShopActivity.class);
            startActivity(intent);
        });

        // 好友 — 需要登录
        btnFriends.setOnClickListener(v -> {
            if (!checkLogin()) return;
            Intent intent = new Intent(this, FriendActivity.class);
            startActivity(intent);
        });

        // ★ 改动2：联机对战 — 加上登录检查！
        btnOnline.setOnClickListener(v -> {
            if (!checkLogin()) return;   // ← 新增！未登录直接拦住

            String username = etUsername.getText().toString().trim();
            if (username.isEmpty()) {
                Toast.makeText(this, "请输入用户名", Toast.LENGTH_SHORT).show();
                return;
            }
            boolean musicOn = cbMusic.isChecked();
            String musicMode = musicOn ? "ON" : "OFF";

            // 联机对战新流程：直接进入联机大厅
            Intent intent = new Intent(this, OnlineLobbyActivity.class);
            intent.putExtra("username", username);
            intent.putExtra("musicMode", musicMode);
            startActivity(intent);
        });

        // ★ 改动3：退出登录 → 跳回登录页
        btnLogout.setOnClickListener(v -> {
            UserSessionManager.getInstance(this).clearLoginInfo();
            Toast.makeText(this, "已退出登录", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        // ★ 新增：去登录/注册 → 跳到登录页
        btnLogin.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        });
    }

    /** 检查是否已登录，未登录则提示 */
    private boolean checkLogin() {
        if (!UserSessionManager.getInstance(this).isLoggedIn()) {
            Toast.makeText(this, "⚠️ 请先登录后再使用此功能\n点击「登录/注册」按钮进入",
                    Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }
}
