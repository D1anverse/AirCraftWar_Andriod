package edu.hitsz.network;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import edu.hitsz.application.Game;
import edu.hitsz.application.GameEasy;
import edu.hitsz.application.GameNormal;
import edu.hitsz.application.GameHard;
import edu.hitsz.application.GameConfig;
import edu.hitsz.R;

/**
 * 在线对战游戏界面
 *
 * 功能:
 * 1. 创建游戏实例嵌入布局中（支持难度选择、英雄选择、音乐设置）
 * 2. 注册分数变化监听 -> 实时同步给服务器
 * 3. 接收服务器消息 -> 更新对手分数 + 绘制EnemyScore + 处理对手死亡
 * 4. 游戏正常结束(HP=0) -> 发送GAME_OVER -> 等待MATCH_END -> 显示双方最终结果
 * 5. 在游戏画布上叠加绘制对手实时分数
 */
public class GameOnlineActivity extends AppCompatActivity implements Game.GameOverListener {

    // UI控件
    private FrameLayout gameContainer;
    private TextView tvOpponentName;
    private TextView tvOpponentStatus;
    private TextView tvConnStatus;
    private TextView tvEnemyScore;

    // 数据
    private String playerName;
    private String opponentName;
    private String username;           // 原始用户名
    private String musicMode;          // 音乐开关
    private String difficulty = "normal"; // 默认普通难度

    // 对手数据
    private int opponentScore = 0;
    private boolean opponentDead = false;
    private boolean selfDead = false;
    private int myFinalScore = 0;
    private int lastSentScore = -1;
    private boolean matchEnded = false;  // 防止重复弹窗

    // 游戏视图
    private Game gameView;

    // Socket客户端
    private SocketClient socketClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_online);

        // 获取传入参数
        Intent intent = getIntent();
        playerName = intent.getStringExtra("playerName");
        opponentName = intent.getStringExtra("opponentName");
        username = intent.getStringExtra("username");
        musicMode = intent.getStringExtra("musicMode");
        if (musicMode == null) musicMode = "ON";
        if (username == null || username.isEmpty()) username = playerName;

        // 绑定UI控件
        gameContainer = findViewById(R.id.game_view_container);
        tvOpponentName = findViewById(R.id.tv_opponent_name);
        tvOpponentStatus = findViewById(R.id.tv_opponent_status);
        tvConnStatus = findViewById(R.id.tv_conn_status);
        tvEnemyScore = findViewById(R.id.tv_enemy_score);

        // 显示对手名称和状态
        tvOpponentName.setText(opponentName);
        tvOpponentStatus.setText("[Alive]");
        tvOpponentStatus.setTextColor(0xFF88FF88);

        // 连接状态
        tvConnStatus.setText("[Connected]");
        tvConnStatus.setTextColor(0xFF00FF00);

        // 初始化游戏视图（核心！）
        initGameView();

        // 设置网络监听
        setupNetworkListener();
    }

    /**
     * 创建游戏视图并注册回调
     */
    private void initGameView() {
        // 根据难度创建对应游戏子类
        switch (difficulty) {
            case "easy":
                gameView = new GameEasy(this, musicMode, username, this);
                break;
            case "hard":
                gameView = new GameHard(this, musicMode, username, this);
                break;
            default:
                gameView = new GameNormal(this, musicMode, username, this);
                break;
        }

        // 注册分数变化监听器 -> 分数改变时自动发给服务器
        gameView.setOnScoreChangedListener(score -> {
            if (!selfDead && score != lastSentScore) {
                lastSentScore = score;
                socketClient.sendScoreUpdate(score);
            }
        });

        // 把游戏视图放入容器
        gameContainer.addView(gameView);
    }

    /**
     * 设置网络消息监听
     */
    private void setupNetworkListener() {
        socketClient = SocketClient.getInstance();

        socketClient.setMessageListener(new SocketClient.OnMessageReceivedListener() {
            @Override
            public void onMessageReceived(final String type, final String body) {
                runOnUiThread(() -> handleServerMessage(type, body));
            }

            @Override
            public void onDisconnected() {
                runOnUiThread(() -> {
                    tvConnStatus.setText("[Disconnected]");
                    tvConnStatus.setTextColor(0xFFFF0000);
                    Toast.makeText(GameOnlineActivity.this,
                            "Connection lost!", Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * 处理服务器发来的每条消息
     */
    private void handleServerMessage(String type, String body) {
        switch (type) {

            case "OPPONENT_SCORE":
                try {
                    opponentScore = Integer.parseInt(body);
                    // 同步到Game画面进行绘制
                    if (gameView != null) {
                        gameView.setEnemyScore(opponentScore);
                    }
                    // 更新UI显示的实时分数
                    tvEnemyScore.setText("Score: " + opponentScore);
                } catch (NumberFormatException ignored) {}
                break;

            case "OPPONENT_DEAD":
                // 对手死亡
                opponentDead = true;
                try {
                    opponentScore = Integer.parseInt(body);
                } catch (NumberFormatException ignored) {}

                tvOpponentStatus.setText("[DEAD]");
                tvOpponentStatus.setTextColor(0xFFFF6666);

                Toast.makeText(this,
                        opponentName + " DEAD! Score: " + opponentScore,
                        Toast.LENGTH_SHORT).show();
                break;

            case "MATCH_END":
                // 双方对局完全结束 -> 收到对手最终分数
                if (matchEnded) return;  // 防止重复
                // 注意：不在这里设置 matchEnded=true，让 showFinalResult 来管理
                try {
                    int oppFinal = Integer.parseInt(body);
                    showFinalResult(oppFinal);
                } catch (NumberFormatException e) {
                    showFinalResult(opponentScore);
                }
                break;
        }
    }

    /**
     * 跳转到结果页面（包含双方分数对比）
     */
    private void showFinalResult(int opponentFinalScore) {
        if (isFinishing() || matchEnded) return;

        matchEnded = true;

        // 跳转到结果页面
        Intent intent = new Intent(GameOnlineActivity.this, GameResultActivity.class);
        intent.putExtra("myScore", myFinalScore);
        intent.putExtra("enemyScore", opponentFinalScore);
        intent.putExtra("myName", playerName);
        intent.putExtra("enemyName", opponentName);
        startActivity(intent);

        // 关闭游戏页面
        finish();
    }

    // 实现 Game.GameOverListener 接口

    /**
     * 我的游戏结束回调（英雄机HP归零时由Game类触发）
     */
    @Override
    public void onGameOver(int score, String userName) {
        selfDead = true;
        myFinalScore = score;

        // 清除enemyScore绘制
        if (gameView != null) {
            gameView.clearEnemyScore();
        }

        socketClient.sendGameOver(score);

        // 如果对手也已经死亡，等待MATCH_END消息来展示最终结果
        // （如果对方网络断开收不到MATCH_END，给一个超时兜底）
        if (opponentDead && !matchEnded) {
            gameContainer.postDelayed(() -> {
                if (!matchEnded && !isFinishing()) {
                    showFinalResult(opponentScore);
                }
            }, 3000);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gameView != null) {
            gameView.clearEnemyScore();
        }
    }
}
