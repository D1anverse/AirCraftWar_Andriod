package edu.hitsz.application;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;
import java.util.List;
import edu.hitsz.dao.PlayerDataManager;
import edu.hitsz.dao.Score;
import edu.hitsz.dao.ScoreDAOImpl;
import edu.hitsz.R;
import edu.hitsz.network.SocketClient;

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

            // 计算并同步代币
            int earnedCoins = calculateCoins(currentScore, currentDifficulty);
            syncCoinsToServer(earnedCoins);
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

    /**
     * 根据难度系数计算代币
     * 简单=1:1，普通=1:1.5，困难=1:2
     */
    private int calculateCoins(int score, String difficulty) {
        switch (difficulty) {
            case "easy":
                return score;
            case "hard":
                return score * 2;
            case "normal":
            default:
                return (int) (score * 1.5f);
        }
    }

    /**
     * 同步代币到服务器
     * 优先实时同步，如果未连接则本地缓存
     * 同时写入本地 SQLite，确保商店能读取到最新代币
     */
    private void syncCoinsToServer(int coins) {
        UserSessionManager session = UserSessionManager.getInstance(this);
        int userId = session.getUserId();

        // 先显示获得的代币提示
        Toast.makeText(ScoreActivity.this, "获得 +" + coins + " 代币！", Toast.LENGTH_SHORT).show();

        // ★ 同时写入本地 SQLite，确保商店页面能立即看到最新代币
        if (userId > 0) {
            PlayerDataManager localDB = new PlayerDataManager(this);
            localDB.addCoins(userId, coins);
        }

        if (!session.isLoggedIn() || userId <= 0) {
            Log.e("ScoreActivity", "用户未登录，代币仅本地缓存");
            return;
        }

        SocketClient client = SocketClient.getInstance();

        if (client.connected()) {
            // 在线模式：直接同步
            // 先同步待缓存的代币
            int pendingCoins = session.getPendingCoins();
            if (pendingCoins > 0) {
                client.sendMessage("SYNC_COINS", userId + "," + pendingCoins);
            }
            // 再同步当前代币
            client.sendMessage("SYNC_COINS", userId + "," + coins);

            client.setMessageListener(new SocketClient.OnMessageReceivedListener() {
                @Override
                public void onMessageReceived(String type, String body) {
                    if ("SYNC_OK".equals(type)) {
                        runOnUiThread(() -> {
                            int totalCoins = coins + session.getPendingCoins();
                            session.saveServerCoins(session.getCachedCoins() + totalCoins);
                            session.clearPendingCoins();
                            Log.d("ScoreActivity", "代币同步成功: +" + totalCoins);
                        });
                    }
                }

                @Override
                public void onDisconnected() {
                    // 离线了，缓存代币
                    runOnUiThread(() -> {
                        session.addPendingCoins(coins);
                        Log.e("ScoreActivity", "连接断开，代币已本地缓存");
                    });
                }
            });
        } else {
            // 离线模式：本地缓存
            session.addPendingCoins(coins);
            Log.d("ScoreActivity", "离线模式，代币已本地缓存，累计: " + session.getPendingCoins());
        }
    }
}
