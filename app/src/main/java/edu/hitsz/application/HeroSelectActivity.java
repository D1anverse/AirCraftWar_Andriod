package edu.hitsz.application;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import edu.hitsz.aircraft.HeroType;
import edu.hitsz.R;
import edu.hitsz.network.OnlineLobbyActivity;

public class HeroSelectActivity extends AppCompatActivity {
    private RadioGroup radioGroupHero;
    private ImageView ivHeroPreview;
    private TextView tvHeroName;
    private TextView tvHeroDesc;
    private Button btnConfirm;
    private Button btnBack;

    private HeroType selectedHeroType = HeroType.HERO_PRO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hero_select);

        // 获取从 MenuActivity 传递的参数
        Intent intent = getIntent();
        String username = intent.getStringExtra("username");
        String difficulty = intent.getStringExtra("difficulty");
        String musicMode = intent.getStringExtra("musicMode");

        initViews();
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
            // 保存选择的英雄机类型到 GameConfig
            GameConfig.getInstance().setSelectedHeroType(selectedHeroType);

            // 检查是否为联机模式
            Intent srcIntent = getIntent();
            boolean isOnlineMode = srcIntent.getBooleanExtra("isOnlineMode", false);

            if (isOnlineMode) {
                // 联机模式：跳转到联机大厅
                Intent intent = new Intent(HeroSelectActivity.this, OnlineLobbyActivity.class);
                intent.putExtra("musicMode", musicMode);
                intent.putExtra("username", username);
                intent.putExtra("heroType", selectedHeroType.getTypeId());
                startActivity(intent);
            } else {
                // 普通模式：跳转到游戏界面
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
