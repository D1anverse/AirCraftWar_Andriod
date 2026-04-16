package edu.hitsz.application;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import edu.hitsz.application.Game;
import edu.hitsz.application.GameEasy;
import edu.hitsz.application.GameHard;
import edu.hitsz.application.GameNormal;
import edu.hitsz.R;

public class GameActivity extends AppCompatActivity implements Game.GameOverListener {
    private Game gameView;
    private String difficulty = "normal";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String username = intent.getStringExtra("username");
        difficulty = intent.getStringExtra("difficulty");
        String musicMode = intent.getStringExtra("musicMode");

        if (difficulty == null) {
            difficulty = "normal";
        }

        switch (difficulty) {
            case "easy":
                gameView = new GameEasy(this, musicMode, username, this);
                break;
            case "normal":
                gameView = new GameNormal(this, musicMode, username, this);
                break;
            case "hard":
                gameView = new GameHard(this, musicMode, username, this);
                break;
            default:
                gameView = new GameEasy(this, musicMode, username, this);
        }
        setContentView(gameView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameView != null) gameView.resumeGame();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (gameView != null) gameView.pauseGame();
    }

    @Override
    public void onGameOver(int score, String userName) {
        Intent intent = new Intent(this, ScoreActivity.class);
        intent.putExtra("score", score);
        intent.putExtra("userName", userName);
        intent.putExtra("difficulty", difficulty);
        startActivity(intent);
        finish();
    }
}