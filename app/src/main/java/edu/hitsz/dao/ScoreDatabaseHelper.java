package edu.hitsz.dao;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class ScoreDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "game_scores.db";
    private static final int DATABASE_VERSION = 3;

    public static final String TABLE_EASY = "scores_easy";
    public static final String TABLE_NORMAL = "scores_normal";
    public static final String TABLE_HARD = "scores_hard";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_USERNAME = "username";
    public static final String COLUMN_SCORE = "score";
    public static final String COLUMN_TIME = "time";
    public static final String TABLE_PLAYER_DATA = "player_data";

    private static final String CREATE_TABLE_EASY =
            "CREATE TABLE " + TABLE_EASY + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_USERNAME + " TEXT NOT NULL, " +
                    COLUMN_SCORE + " INTEGER NOT NULL, " +
                    COLUMN_TIME + " TEXT NOT NULL)";

    private static final String CREATE_TABLE_NORMAL =
            "CREATE TABLE " + TABLE_NORMAL + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_USERNAME + " TEXT NOT NULL, " +
                    COLUMN_SCORE + " INTEGER NOT NULL, " +
                    COLUMN_TIME + " TEXT NOT NULL)";

    private static final String CREATE_TABLE_HARD =
            "CREATE TABLE " + TABLE_HARD + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_USERNAME + " TEXT NOT NULL, " +
                    COLUMN_SCORE + " INTEGER NOT NULL, " +
                    COLUMN_TIME + " TEXT NOT NULL)";

    // 创建玩家数据表SQL
    private static final String CREATE_TABLE_PLAYER_DATA =
            "CREATE TABLE " + TABLE_PLAYER_DATA + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "user_id INTEGER NOT NULL UNIQUE, " +  // 对应服务器的用户ID
                    "coins INTEGER DEFAULT 0, " +
                    "unlocked_pro INTEGER DEFAULT 0, " +
                    "unlocked_promax INTEGER DEFAULT 0)";

    public ScoreDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_EASY);
        db.execSQL(CREATE_TABLE_NORMAL);
        db.execSQL(CREATE_TABLE_HARD);
        db.execSQL(CREATE_TABLE_PLAYER_DATA);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            // 只追加新表，不动旧数据
            db.execSQL(CREATE_TABLE_PLAYER_DATA);
        }
    }

    public static String getTableName(String difficulty) {
        switch (difficulty) {
            case "easy":
                return TABLE_EASY;
            case "normal":
                return TABLE_NORMAL;
            case "hard":
                return TABLE_HARD;
            default:
                return TABLE_NORMAL;
        }
    }
}