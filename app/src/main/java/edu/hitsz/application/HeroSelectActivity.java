package edu.hitsz.application;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import edu.hitsz.aircraft.HeroType;
import edu.hitsz.R;
import edu.hitsz.dao.PlayerDataManager;
import edu.hitsz.entity.PlayerData;
import edu.hitsz.network.OnlineLobbyActivity;

public class HeroSelectActivity extends AppCompatActivity {
    private RadioGroup radioGroupHero;
    private ImageView ivHeroPreview;
    private TextView tvHeroName;
    private TextView tvHeroDesc;
    private Button btnConfirm;
    private Button btnBack;

    private HeroType selectedHeroType = HeroType.HERO_PRO;

    private PlayerDataManager playerDataManager;
    private PlayerData currentPlayerData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hero_select);

        // 获取从 MenuActivity 传递的参数
        Intent intent = getIntent();
        String username = intent.getStringExtra("username");
        String difficulty = intent.getStringExtra("difficulty");
        String musicMode = intent.getStringExtra("musicMode");

        // 初始化玩家数据管理器（新增）
        playerDataManager = new PlayerDataManager(this);
        UserSessionManager session = UserSessionManager.getInstance(this);
        if (session.isLoggedIn() && session.getUserId() > 0) {
            currentPlayerData = playerDataManager.getOrCreatePlayerData(session.getUserId());
        }

        initViews();
        setupRadioLockState();  // 新增：设置RadioButton锁定状态
        setupListeners(username, difficulty, musicMode);
        updateHeroDisplay(selectedHeroType);
    }

    private void initViews() {
        radioGroupHero = findViewById(R.id.radio_group_hero);
        ivHeroPreview = findViewById(R.id.iv_hero_preview);
        tvHeroName = findViewById(R.id.tv_hero_name);
        tvHeroDesc = findViewById(R.id.tv_hero_desc);
        btnConfirm = findViewById(R.id.btn_confirm);
        btnBack = findViewById(R.id.btn_back);
    }

    /**
     * 【新增】根据解锁状态设置 RadioButton 的可用性
     * 未解锁的飞机显示锁图标且不可选中
     */
    private void setupRadioLockState() {
        if (currentPlayerData == null) return; // 未登录时不限制

        RadioButton rbBasic = findViewById(R.id.radio_hero_basic);
        RadioButton rbPro = findViewById(R.id.radio_hero_pro);
        RadioButton rbPromax = findViewById(R.id.radio_hero_promax);

        // BASIC 默认解锁，永远可选
        // rbBasic 不需要改

        // PRO
        if (!currentPlayerData.isUnlockedPro()) {
            rbPro.setText("🔒 进阶型 (需5000代币)");
            // 不禁用RadioButton本身，而是在confirm时拦截
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

    private void setupListeners(String username, String difficulty, String musicMode) {
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
            // ========== 【新增】解锁状态检查 ==========
            if (currentPlayerData != null) {
                if (selectedHeroType == HeroType.HERO_PRO && !currentPlayerData.isUnlockedPro()) {
                    Toast.makeText(this, "进阶型尚未解锁！\n前往商店花费5000代币解锁",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                if (selectedHeroType == HeroType.HERO_PROMAX && !currentPlayerData.isUnlockedPromax()) {
                    Toast.makeText(this, "旗舰型尚未解锁！\n前往商店花费15000代币解锁",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            // ========== 解锁检查结束 ==========

            // 保存选择的英雄机类型到 GameConfig
            GameConfig.getInstance().setSelectedHeroType(selectedHeroType);

            // 检查是否为联机模式
            Intent srcIntent = getIntent();
            boolean isOnlineMode = srcIntent.getBooleanExtra("isOnlineMode", false);

            if (isOnlineMode) {
                Intent intent = new Intent(HeroSelectActivity.this, OnlineLobbyActivity.class);
                intent.putExtra("musicMode", musicMode);
                intent.putExtra("username", username);
                intent.putExtra("heroType", selectedHeroType.getTypeId());
                startActivity(intent);
            } else {
                Intent gameIntent = new Intent(HeroSelectActivity.this, GameActivity.class);
                gameIntent.putExtra("username", username);
                gameIntent.putExtra("difficulty", difficulty);
                gameIntent.putExtra("musicMode", musicMode);
                gameIntent.putExtra("heroType", selectedHeroType.getTypeId());
                startActivity(gameIntent);
            }
            finish();
        });

        btnBack.setOnClickListener(v -> finish());
    }

    private void updateHeroDisplay(HeroType heroType) {
        ivHeroPreview.setImageBitmap(heroType.getImage());
        tvHeroName.setText(heroType.getDisplayName());
        tvHeroDesc.setText(heroType.getDescription());
    }
}
