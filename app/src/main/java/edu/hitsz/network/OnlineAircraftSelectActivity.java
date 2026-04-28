package edu.hitsz.network;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.os.Looper;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import edu.hitsz.R;
import edu.hitsz.aircraft.HeroType;
import edu.hitsz.dao.PlayerDataManager;
import edu.hitsz.entity.PlayerData;
import edu.hitsz.application.UserSessionManager;

/**
 * 联机对战 - 飞机选择界面
 * 功能: 选择战机（根据解锁状态），双方确认后进入对战
 */
public class OnlineAircraftSelectActivity extends AppCompatActivity {

    public static final String EXTRA_PLAYER_NAME = "playerName";
    public static final String EXTRA_OPPONENT_NAME = "opponentName";
    public static final String EXTRA_DIFFICULTY = "difficulty";
    public static final String EXTRA_MUSIC_MODE = "musicMode";
    public static final String EXTRA_USERNAME = "username";

    private RadioGroup radioGroupHero;
    private ImageView ivHeroPreview;
    private TextView tvHeroName;
    private TextView tvHeroDesc;
    private TextView tvOpponentStatus;
    private Button btnConfirm;
    private Button btnBack;

    private HeroType selectedHeroType = HeroType.HERO_BASIC;

    private PlayerDataManager playerDataManager;
    private PlayerData currentPlayerData;

    private String playerName;
    private String opponentName;
    private String difficulty;
    private String musicMode;
    private String username;
    private boolean opponentConfirmed = false;
    private boolean selfConfirmed = false;

    private SocketClient socketClient;
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online_aircraft_select);

        Intent intent = getIntent();
        playerName = intent.getStringExtra(EXTRA_PLAYER_NAME);
        opponentName = intent.getStringExtra(EXTRA_OPPONENT_NAME);
        difficulty = intent.getStringExtra(EXTRA_DIFFICULTY);
        musicMode = intent.getStringExtra(EXTRA_MUSIC_MODE);
        username = intent.getStringExtra(EXTRA_USERNAME);
        if (musicMode == null) musicMode = "ON";
        if (username == null || username.isEmpty()) username = playerName;

        // 初始化玩家数据管理器（用于检查解锁状态）
        playerDataManager = new PlayerDataManager(this);
        UserSessionManager session = UserSessionManager.getInstance(this);
        if (session.isLoggedIn() && session.getUserId() > 0) {
            currentPlayerData = playerDataManager.getOrCreatePlayerData(session.getUserId());
        }

        initViews();
        setupRadioLockState();
        setupListeners();
        setupSocketListener();
    }

    private void initViews() {
        radioGroupHero = findViewById(R.id.radio_group_hero);
        ivHeroPreview = findViewById(R.id.iv_hero_preview);
        tvHeroName = findViewById(R.id.tv_hero_name);
        tvHeroDesc = findViewById(R.id.tv_hero_desc);
        tvOpponentStatus = findViewById(R.id.tv_opponent_status);
        btnConfirm = findViewById(R.id.btn_confirm);
        btnBack = findViewById(R.id.btn_back);

        // 默认选中基础型
        ((RadioButton) findViewById(R.id.radio_hero_basic)).setChecked(true);
        updateHeroDisplay(selectedHeroType);
    }

    /**
     * 根据解锁状态设置 RadioButton 的显示
     */
    private void setupRadioLockState() {
        if (currentPlayerData == null) return; // 未登录时不限制

        RadioButton rbBasic = findViewById(R.id.radio_hero_basic);
        RadioButton rbPro = findViewById(R.id.radio_hero_pro);
        RadioButton rbPromax = findViewById(R.id.radio_hero_promax);

        // PRO
        if (!currentPlayerData.isUnlockedPro()) {
            rbPro.setText("🔒 进阶型 (需5000代币)");
        } else {
            rbPro.setText("进阶型");
        }

        // PROMAX
        if (!currentPlayerData.isUnlockedPromax()) {
            rbPromax.setText("🔒 旗舰型 (需15000代币)");
        } else {
            rbPromax.setText("旗舰型");
        }
    }

    private void setupListeners() {
        radioGroupHero.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_hero_basic) {
                selectedHeroType = HeroType.HERO_BASIC;
            } else if (checkedId == R.id.radio_hero_pro) {
                selectedHeroType = HeroType.HERO_PRO;
            } else if (checkedId == R.id.radio_hero_promax) {
                selectedHeroType = HeroType.HERO_PROMAX;
            }
            updateHeroDisplay(selectedHeroType);
        });

        btnConfirm.setOnClickListener(v -> {
            // 解锁状态检查
            if (currentPlayerData != null) {
                if (selectedHeroType == HeroType.HERO_PRO && !currentPlayerData.isUnlockedPro()) {
                    Toast.makeText(this, "进阶型尚未解锁！", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (selectedHeroType == HeroType.HERO_PROMAX && !currentPlayerData.isUnlockedPromax()) {
                    Toast.makeText(this, "旗舰型尚未解锁！", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // 发送飞机选择到服务器
            selfConfirmed = true;
            socketClient.sendAircraftSelect(selectedHeroType.getTypeId());
            btnConfirm.setEnabled(false);
            btnConfirm.setText("已确认，等待对手...");
            tvOpponentStatus.setVisibility(View.VISIBLE);
            tvOpponentStatus.setText("等待 [" + opponentName + "] 选择战机...");
            // 注意：不要在这里检查 opponentConfirmed，只依赖服务器发送的 READY_TO_FIGHT
        });

        btnBack.setOnClickListener(v -> {
            socketClient.leaveRoom();
            socketClient.disconnect();
            finish();
        });
    }

    private void updateHeroDisplay(HeroType heroType) {
        ivHeroPreview.setImageBitmap(heroType.getImage());
        tvHeroName.setText(heroType.getDisplayName());
        tvHeroDesc.setText(heroType.getDescription());
    }

    private void setupSocketListener() {
        socketClient = SocketClient.getInstance();

        socketClient.setMessageListener(new SocketClient.OnMessageReceivedListener() {
            @Override
            public void onMessageReceived(String type, String body) {
                mainHandler.post(() -> handleServerMessage(type, body));
            }

            @Override
            public void onDisconnected() {
                mainHandler.post(() -> {
                    Toast.makeText(OnlineAircraftSelectActivity.this,
                            "与服务器断开连接", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private void handleServerMessage(String type, String body) {
        switch (type) {
            case "OPPONENT_AIRCRAFT_SELECTED":
                // 对手已选择战机，只更新状态，不自动进入
                opponentConfirmed = true;
                tvOpponentStatus.setText("[" + opponentName + "] 已选择战机");
                tvOpponentStatus.setTextColor(0xFF00FF00);
                // 注意：不要在这里检查 selfConfirmed 进入游戏，只依赖 READY_TO_FIGHT
                break;

            case "READY_TO_FIGHT":
                // 只有收到 READY_TO_FIGHT 才进入战斗（双方都已确认）
                goToGame();
                break;

            case "OPPONENT_LEFT":
                // 对手离开了
                Toast.makeText(this, opponentName + " 已离开房间", Toast.LENGTH_SHORT).show();
                socketClient.disconnect();
                finish();
                break;

            case "ROOM_CANCELLED":
                Toast.makeText(this, "房间已取消", Toast.LENGTH_SHORT).show();
                socketClient.disconnect();
                finish();
                break;
        }
    }

    private void goToGame() {
        // 保存选择的英雄机类型到 GameConfig
        edu.hitsz.application.GameConfig.getInstance().setSelectedHeroType(selectedHeroType);

        Intent intent = new Intent(OnlineAircraftSelectActivity.this, GameOnlineActivity.class);
        intent.putExtra("playerName", playerName);
        intent.putExtra("opponentName", opponentName);
        intent.putExtra("difficulty", difficulty);
        intent.putExtra("heroType", selectedHeroType.getTypeId());
        intent.putExtra("musicMode", musicMode);
        intent.putExtra("username", username);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 不主动断开连接
    }
}
