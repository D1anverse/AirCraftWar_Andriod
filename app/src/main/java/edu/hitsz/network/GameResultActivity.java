package edu.hitsz.network;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import edu.hitsz.R;
import edu.hitsz.application.MenuActivity;

/**
 * 联机对战结果页面
 * 显示双方最终分数对比
 */
public class GameResultActivity extends AppCompatActivity {

    private TextView tvResult;
    private TextView tvMyScore;
    private TextView tvEnemyScore;
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
        btnBackHome = findViewById(R.id.btn_back_home);

        // 显示分数
        tvMyScore.setText(String.valueOf(myScore));
        tvEnemyScore.setText(String.valueOf(enemyScore));

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
            // 断开网络连接
            SocketClient.getInstance().disconnect();

            // 跳转到主页面，关闭所有其他Activity
            Intent homeIntent = new Intent(this, MenuActivity.class);
            homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(homeIntent);
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        // 禁用返回键，点击按钮返回
        btnBackHome.performClick();
    }
}
