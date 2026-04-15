package edu.hitsz.application;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import edu.hitsz.dao.Score;
import edu.hitsz.dao.ScoreDAOImpl;
import edu.hitsz.R;

public class ScoreActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ScoreAdapter adapter;
    private ScoreDAOImpl scoreDAO;
    private Button btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_score);

        scoreDAO = new ScoreDAOImpl(this);

        Intent intent = getIntent();
        if (intent.hasExtra("score") && intent.hasExtra("userName")) {
            int currentScore = intent.getIntExtra("score", 0);
            String userName = intent.getStringExtra("userName");
            scoreDAO.addScore(new Score(userName, currentScore));
        }

        recyclerView = findViewById(R.id.recycler_view);
        btnBack = findViewById(R.id.btn_back);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

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
    }

    public void refreshData() {
        List<Score> allScores = scoreDAO.getAllScores();
        allScores.sort((s1, s2) -> Integer.compare(s2.getScore(), s1.getScore()));
        adapter.updateData(allScores);
    }
}
