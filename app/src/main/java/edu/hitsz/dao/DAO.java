package edu.hitsz.dao;
import java.util.List;

public interface DAO {
    void addScore(Score score);

    List<Score> getAllScores();

    void printAllScores();

    List<Score> getTopScores(int topN);

    void printLeaderboard(int topN);

    List<Score> getScoresByUsername(String username);

    void printUserScores(String username);

    void clearAllScores();
}
