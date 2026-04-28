package edu.hitsz.network;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import edu.hitsz.R;
import edu.hitsz.application.MenuActivity;
import edu.hitsz.application.UserSessionManager;
import edu.hitsz.dao.PlayerDataManager;
import edu.hitsz.entity.PlayerData;

/**
 * 联机对战结果页面
 * 显示双方最终分数对比
 */
public class GameResultActivity extends AppCompatActivity {

    private TextView tvResult;
    private TextView tvMyScore;
    private TextView tvEnemyScore;
    private TextView tvCoinsEarned;
    private Button btnBackHome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_result);

        // 获取传递的参数
        Intent intent = getIntent();
        int myScore = intent.getIntExtra("myScore", 0);
        int enemyScore = intent.getIntExtra("enemyScore", 0);
        String myName = intent.getStringExtra("myName");
        String enemyName = intent.getStringExtra("enemyName");

        // 绑定控件
        tvResult = findViewById(R.id.tv_result);
        tvMyScore = findViewById(R.id.tv_my_score);
        tvEnemyScore = findViewById(R.id.tv_enemy_score);
        tvCoinsEarned = findViewById(R.id.tv_coins_earned);
        btnBackHome = findViewById(R.id.btn_back_home);

        // 显示分数
        tvMyScore.setText(String.valueOf(myScore));
        tvEnemyScore.setText(String.valueOf(enemyScore));

        // 计算并同步代币
        UserSessionManager session = UserSessionManager.getInstance(GameResultActivity.this);
        if (session.isLoggedIn() && session.getUserId() > 0) {
            // 联机难度系数固定为1.5
            final int earnedCoins = (int) (myScore * 1.5f);
            tvCoinsEarned.setText("获得代币: +" + earnedCoins);
            tvCoinsEarned.setVisibility(View.VISIBLE);

            syncCoinsToServer(session.getUserId(), earnedCoins);
        }

        // 判断胜负
        String resultText;
        int resultColor;
        if (myScore > enemyScore) {
            resultText = "\u60A8\u8D62\u4E86\uFF01";
            resultColor = 0xFF00CC00;
        } else if (myScore < enemyScore) {
            resultText = "\u60A8\u8F93\u4E86";
            resultColor = 0xFFCC2222;
        } else {
            resultText = "\u5E73\u624B";
            resultColor = 0xFFCCCC00;
        }
        tvResult.setText(resultText);
        tvResult.setTextColor(resultColor);

        // 返回主页按钮
        btnBackHome.setOnClickListener(v -> {
            // 发送离开房间消息，通知服务器重置状态
            SocketClient client = SocketClient.getInstance();
            if (client.connected()) {
                client.sendMessage("LEAVE_ROOM", "");
            }

            // 跳转到主页面，关闭所有其他Activity
            Intent homeIntent = new Intent(this, MenuActivity.class);
            homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(homeIntent);
            finish();
        });
    }

    /**
     * 同步代币到服务器
     */
    private void syncCoinsToServer(int userId, int earnedCoins) {
        SocketClient client = SocketClient.getInstance();
        if (!client.connected()) {
            android.util.Log.e("GameResult", "未连接服务器，代币同步失败");
            // 即使离线也要更新本地SQLite
            updateLocalCoins(userId, earnedCoins);
            return;
        }

        // 使用一次性查询回调，避免覆盖原有监听器
        client.setQueryCallback(new SocketClient.OnMessageReceivedListener() {
            @Override
            public void onMessageReceived(String type, String body) {
                if ("SYNC_OK".equals(type)) {
                    runOnUiThread(() -> {
                        // 更新本地SQLite和缓存
                        updateLocalCoins(userId, earnedCoins);
                        android.util.Log.d("GameResult", "代币同步成功: +" + earnedCoins);
                    });
                }
                client.clearQueryCallback();
            }

            @Override
            public void onDisconnected() {
                android.util.Log.e("GameResult", "连接断开，尝试更新本地代币");
                runOnUiThread(() -> updateLocalCoins(userId, earnedCoins));
                client.clearQueryCallback();
            }
        });

        client.sendMessage("SYNC_COINS", userId + "," + earnedCoins);
    }

    /**
     * 更新本地代币（SQLite + SharedPreferences）
     */
    private void updateLocalCoins(int userId, int earnedCoins) {
        try {
            // 更新SQLite
            PlayerDataManager db = new PlayerDataManager(this);
            PlayerData pd = db.getOrCreatePlayerData(userId);
            int newCoins = pd.getCoins() + earnedCoins;
            pd.setCoins(newCoins);
            db.insertOrUpdate(pd);

            // 更新缓存
            UserSessionManager session = UserSessionManager.getInstance(this);
            session.saveServerCoins(newCoins);

            android.util.Log.d("GameResult", "本地代币已更新: " + newCoins);
        } catch (Exception e) {
            android.util.Log.e("GameResult", "更新本地代币失败: " + e.getMessage());
        }
    }

    @Override
    public void onBackPressed() {
        // 禁用返回键，点击按钮返回
        btnBackHome.performClick();
    }
}
