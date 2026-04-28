package edu.hitsz.network;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import edu.hitsz.R;

/**
 * 联机对战 - 房间等待界面
 * 功能: 选择难度，等待双方难度匹配后进入飞机选择
 */
public class OnlineRoomWaitingActivity extends AppCompatActivity {

    public static final String EXTRA_PLAYER_NAME = "playerName";
    public static final String EXTRA_OPPONENT_NAME = "opponentName";
    public static final String EXTRA_MUSIC_MODE = "musicMode";
    public static final String EXTRA_USERNAME = "username";

    private RadioGroup rgDifficulty;
    private RadioButton rbEasy, rbNormal, rbHard;
    private TextView tvStatus;
    private TextView tvOpponentDifficulty;
    private LinearLayout layoutOpponent;
    private TextView tvMatchStatus;
    private Button btnConfirm;
    private Button btnBack;

    private String playerName;
    private String opponentName;
    private String username;
    private String musicMode;
    private String selectedDifficulty = null;
    private String opponentDifficulty = null;
    private boolean difficultyMatched = false;

    private SocketClient socketClient;
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online_room_waiting);

        Intent intent = getIntent();
        playerName = intent.getStringExtra(EXTRA_PLAYER_NAME);
        opponentName = intent.getStringExtra(EXTRA_OPPONENT_NAME);
        username = intent.getStringExtra(EXTRA_USERNAME);
        musicMode = intent.getStringExtra(EXTRA_MUSIC_MODE);
        if (musicMode == null) musicMode = "ON";
        if (username == null || username.isEmpty()) username = playerName;

        initViews();
        setupListeners();
        setupSocketListener();
    }

    private void initViews() {
        rgDifficulty = findViewById(R.id.rg_difficulty);
        rbEasy = findViewById(R.id.rb_easy);
        rbNormal = findViewById(R.id.rb_normal);
        rbHard = findViewById(R.id.rb_hard);
        tvStatus = findViewById(R.id.tv_status);
        tvOpponentDifficulty = findViewById(R.id.tv_opponent_difficulty);
        layoutOpponent = findViewById(R.id.layout_opponent);
        tvMatchStatus = findViewById(R.id.tv_match_status);
        btnConfirm = findViewById(R.id.btn_confirm);
        btnBack = findViewById(R.id.btn_back);

        tvStatus.setText("[" + opponentName + "] 已加入房间，请选择难度");
        tvStatus.setTextColor(Color.CYAN);
    }

    private void setupListeners() {
        rgDifficulty.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_easy) {
                selectedDifficulty = "easy";
            } else if (checkedId == R.id.rb_normal) {
                selectedDifficulty = "normal";
            } else if (checkedId == R.id.rb_hard) {
                selectedDifficulty = "hard";
            }
            updateConfirmButton();
        });

        btnConfirm.setOnClickListener(v -> {
            if (selectedDifficulty == null) {
                Toast.makeText(this, "请先选择难度", Toast.LENGTH_SHORT).show();
                return;
            }
            // 发送难度选择到服务器
            socketClient.sendDifficultySelect(selectedDifficulty);
            btnConfirm.setEnabled(false);
            btnConfirm.setText("已确认，等待对手...");
            tvStatus.setText("等待 [" + opponentName + "] 选择难度...");
        });

        btnBack.setOnClickListener(v -> {
            socketClient.leaveRoom();
            socketClient.disconnect();
            finish();
        });
    }

    private void updateConfirmButton() {
        btnConfirm.setEnabled(selectedDifficulty != null);
        if (selectedDifficulty != null) {
            btnConfirm.setText("确认难度: " + getDifficultyDisplayName(selectedDifficulty));
        } else {
            btnConfirm.setText("确认难度");
        }
    }

    private String getDifficultyDisplayName(String diff) {
        switch (diff) {
            case "easy": return "简单";
            case "normal": return "普通";
            case "hard": return "困难";
            default: return diff;
        }
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
                    Toast.makeText(OnlineRoomWaitingActivity.this,
                            "与服务器断开连接", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private void handleServerMessage(String type, String body) {
        switch (type) {
            case "OPPONENT_DIFFICULTY":
                // 对手选择了难度
                opponentDifficulty = body;
                layoutOpponent.setVisibility(View.VISIBLE);
                tvOpponentDifficulty.setText(getDifficultyDisplayName(opponentDifficulty));
                tvOpponentDifficulty.setTextColor(getDifficultyColor(opponentDifficulty));
                break;

            case "DIFFICULTY_MATCHED":
                // 双方难度匹配成功，可以进入飞机选择了
                difficultyMatched = true;
                tvMatchStatus.setVisibility(View.VISIBLE);
                tvMatchStatus.setText("难度匹配成功！");
                tvMatchStatus.setTextColor(Color.GREEN);
                tvStatus.setText("即将进入战机选择...");
                // 延迟进入飞机选择界面
                mainHandler.postDelayed(() -> {
                    if (!isFinishing()) {
                        goToAircraftSelect();
                    }
                }, 1500);
                break;

            case "DIFFICULTY_RETRY":
                // 难度不匹配，要求重新选择
                Toast.makeText(this, "难度不匹配，请重新选择", Toast.LENGTH_SHORT).show();
                btnConfirm.setEnabled(true);
                btnConfirm.setText("确认难度");
                opponentDifficulty = null;
                layoutOpponent.setVisibility(View.GONE);
                break;

            case "OPPONENT_LEFT":
                // 对手离开了
                Toast.makeText(this, opponentName + " 已离开房间", Toast.LENGTH_SHORT).show();
                socketClient.disconnect();
                finish();
                break;

            case "ROOM_CANCELLED":
                // 房间被取消
                Toast.makeText(this, "房间已取消", Toast.LENGTH_SHORT).show();
                socketClient.disconnect();
                finish();
                break;
        }
    }

    private int getDifficultyColor(String diff) {
        switch (diff) {
            case "easy": return Color.parseColor("#88FF88");
            case "normal": return Color.parseColor("#FFFF88");
            case "hard": return Color.parseColor("#FF8888");
            default: return Color.WHITE;
        }
    }

    private void goToAircraftSelect() {
        Intent intent = new Intent(OnlineRoomWaitingActivity.this, OnlineAircraftSelectActivity.class);
        intent.putExtra(OnlineAircraftSelectActivity.EXTRA_PLAYER_NAME, playerName);
        intent.putExtra(OnlineAircraftSelectActivity.EXTRA_OPPONENT_NAME, opponentName);
        intent.putExtra(OnlineAircraftSelectActivity.EXTRA_DIFFICULTY, selectedDifficulty);
        intent.putExtra(OnlineAircraftSelectActivity.EXTRA_MUSIC_MODE, musicMode);
        intent.putExtra(OnlineAircraftSelectActivity.EXTRA_USERNAME, username);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 不主动断开连接，让新Activity继续使用
    }
}
