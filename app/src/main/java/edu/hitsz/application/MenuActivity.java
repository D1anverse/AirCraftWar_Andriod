package edu.hitsz.application;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import edu.hitsz.application.GameConfig;
import edu.hitsz.application.ImageManager;
import edu.hitsz.R;

public class MenuActivity extends AppCompatActivity {
    private EditText etUsername;
    private RadioGroup radioGroup;
    private CheckBox cbMusic;
    private Button btnStart, btnRank;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        GameConfig.init(this);
        ImageManager.init(getResources());

        etUsername = findViewById(R.id.et_username);
        radioGroup = findViewById(R.id.radio_group_difficulty);
        cbMusic = findViewById(R.id.cb_music);
        btnStart = findViewById(R.id.btn_start);
        btnRank = findViewById(R.id.btn_rank);

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

            // 跳转到英雄机选取界面，而不是直接进入游戏
            Intent intent = new Intent(this, HeroSelectActivity.class);
            intent.putExtra("username", username);
            intent.putExtra("difficulty", difficulty);
            intent.putExtra("musicMode", musicMode);
            startActivity(intent);
        });

        btnRank.setOnClickListener(v -> {
            startActivity(new Intent(this, ScoreActivity.class));
        });
    }
}
