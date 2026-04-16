package edu.hitsz.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;

public class ScoreDAOImpl implements DAO {
    private ScoreDatabaseHelper dbHelper;
    private Context context;
    private String currentDifficulty = "normal";

    public ScoreDAOImpl(Context context) {
        this.context = context.getApplicationContext();
        dbHelper = new ScoreDatabaseHelper(this.context);
    }

    public void setDifficulty(String difficulty) {
        this.currentDifficulty = difficulty;
    }

    public String getDifficulty() {
        return currentDifficulty;
    }

    private String getTableName() {
        return ScoreDatabaseHelper.getTableName(currentDifficulty);
    }

    @Override
    public void addScore(Score score) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(ScoreDatabaseHelper.COLUMN_USERNAME, score.getUsername());
        values.put(ScoreDatabaseHelper.COLUMN_SCORE, score.getScore());
        values.put(ScoreDatabaseHelper.COLUMN_TIME, score.getTime());
        long id = db.insert(getTableName(), null, values);
        score.setId((int) id);
        db.close();
    }

    @Override
    public List<Score> getAllScores() {
        List<Score> scoreList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(getTableName(),
                null, null, null, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(ScoreDatabaseHelper.COLUMN_ID));
                String username = cursor.getString(cursor.getColumnIndexOrThrow(ScoreDatabaseHelper.COLUMN_USERNAME));
                int score = cursor.getInt(cursor.getColumnIndexOrThrow(ScoreDatabaseHelper.COLUMN_SCORE));
                String time = cursor.getString(cursor.getColumnIndexOrThrow(ScoreDatabaseHelper.COLUMN_TIME));
                scoreList.add(new Score(id, username, score, time));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return scoreList;
    }

    public void deleteScore(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(getTableName(),
                ScoreDatabaseHelper.COLUMN_ID + " = ?",
                new String[]{String.valueOf(id)});
        db.close();
    }

    @Override
    public void clearAllScores() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(getTableName(), null, null);
        db.close();
    }

    public void clearAllDifficulties() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(ScoreDatabaseHelper.TABLE_EASY, null, null);
        db.delete(ScoreDatabaseHelper.TABLE_NORMAL, null, null);
        db.delete(ScoreDatabaseHelper.TABLE_HARD, null, null);
        db.close();
    }

    @Override
    public List<Score> getTopScores(int topN) {
        List<Score> scoreList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(getTableName(),
                null, null, null, null, null,
                ScoreDatabaseHelper.COLUMN_SCORE + " DESC",
                String.valueOf(topN));

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(ScoreDatabaseHelper.COLUMN_ID));
                String username = cursor.getString(cursor.getColumnIndexOrThrow(ScoreDatabaseHelper.COLUMN_USERNAME));
                int score = cursor.getInt(cursor.getColumnIndexOrThrow(ScoreDatabaseHelper.COLUMN_SCORE));
                String time = cursor.getString(cursor.getColumnIndexOrThrow(ScoreDatabaseHelper.COLUMN_TIME));
                scoreList.add(new Score(id, username, score, time));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return scoreList;
    }

    @Override
    public List<Score> getScoresByUsername(String username) {
        List<Score> scoreList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(getTableName(),
                null,
                ScoreDatabaseHelper.COLUMN_USERNAME + " = ?",
                new String[]{username},
                null, null,
                ScoreDatabaseHelper.COLUMN_SCORE + " DESC");

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(ScoreDatabaseHelper.COLUMN_ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(ScoreDatabaseHelper.COLUMN_USERNAME));
                int score = cursor.getInt(cursor.getColumnIndexOrThrow(ScoreDatabaseHelper.COLUMN_SCORE));
                String time = cursor.getString(cursor.getColumnIndexOrThrow(ScoreDatabaseHelper.COLUMN_TIME));
                scoreList.add(new Score(id, name, score, time));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return scoreList;
    }

    @Override
    public void printAllScores() { }

    @Override
    public void printLeaderboard(int topN) { }

    @Override
    public void printUserScores(String username) { }
}