package edu.hitsz.application;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;
import java.util.List;
import edu.hitsz.dao.Score;
import edu.hitsz.dao.ScoreDAOImpl;
import edu.hitsz.R;

public class ScoreActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ScoreAdapter adapter;
    private ScoreDAOImpl scoreDAO;
    private TabLayout tabLayout;
    private String currentDifficulty = "normal";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_score);

        scoreDAO = new ScoreDAOImpl(this);

        Intent intent = getIntent();
        if (intent.hasExtra("score") && intent.hasExtra("userName")) {
            int currentScore = intent.getIntExtra("score", 0);
            String userName = intent.getStringExtra("userName");
            String difficulty = intent.getStringExtra("difficulty");
            if (difficulty != null) {
                scoreDAO.setDifficulty(difficulty);
                currentDifficulty = difficulty;
            }
            scoreDAO.addScore(new Score(userName, currentScore));
        }

        recyclerView = findViewById(R.id.recycler_view);
        tabLayout = findViewById(R.id.tab_layout);
        Button btnBack = findViewById(R.id.btn_back);
        Button btnClear = findViewById(R.id.btn_clear);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        tabLayout.addTab(tabLayout.newTab().setText("简单"));
        tabLayout.addTab(tabLayout.newTab().setText("普通"));
        tabLayout.addTab(tabLayout.newTab().setText("困难"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        currentDifficulty = "easy";
                        break;
                    case 1:
                        currentDifficulty = "normal";
                        break;
                    case 2:
                        currentDifficulty = "hard";
                        break;
                }
                scoreDAO.setDifficulty(currentDifficulty);
                loadScores();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        List<Score> allScores = scoreDAO.getAllScores();
        allScores.sort((s1, s2) -> Integer.compare(s2.getScore(), s1.getScore()));

        adapter = new ScoreAdapter(this, allScores, scoreDAO);
        recyclerView.setAdapter(adapter);

        btnBack.setOnClickListener(v -> {
            Intent intentBack = new Intent(ScoreActivity.this, MenuActivity.class);
            intentBack.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intentBack);
            finish();
        });

        btnClear.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("清空确认")
                    .setMessage("确定要清空当前难度(" + getDifficultyName() + ")的排行榜吗？")
                    .setPositiveButton("确定", (dialog, which) -> {
                        scoreDAO.clearAllScores();
                        loadScores();
                        Toast.makeText(this, "已清空", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });

        int tabIndex = "easy".equals(currentDifficulty) ? 0 : ("normal".equals(currentDifficulty) ? 1 : 2);
        tabLayout.getTabAt(tabIndex).select();
    }

    private String getDifficultyName() {
        switch (currentDifficulty) {
            case "easy":
                return "简单";
            case "normal":
                return "普通";
            case "hard":
                return "困难";
            default:
                return "普通";
        }
    }

    private void loadScores() {
        List<Score> allScores = scoreDAO.getAllScores();
        allScores.sort((s1, s2) -> Integer.compare(s2.getScore(), s1.getScore()));
        adapter.updateData(allScores);
    }

    public void refreshData() {
        loadScores();
    }
}
