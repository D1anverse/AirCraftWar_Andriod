package edu.hitsz.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import edu.hitsz.entity.PlayerData;

/**
 * 玩家数据 DAO - 管理本地 player_data 表的读写
 */
public class PlayerDataManager {

    private final ScoreDatabaseHelper dbHelper;

    public PlayerDataManager(Context context) {
        dbHelper = new ScoreDatabaseHelper(context);
    }

    /**
     * 获取或初始化玩家数据
     * 如果该 user_id 没有记录，则创建默认记录（0代币，未解锁PRO/PROMAX）
     */
    public PlayerData getOrCreatePlayerData(int userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                ScoreDatabaseHelper.TABLE_PLAYER_DATA,
                null,
                "user_id = ?",
                new String[]{String.valueOf(userId)},
                null, null, null
        );

        PlayerData data;
        if (cursor.moveToFirst()) {
            data = cursorToPlayerData(cursor);
            cursor.close();
        } else {
            cursor.close();
            // 首次使用，创建默认记录
            data = new PlayerData(userId, 0, false, false);
            insertOrUpdate(data);
        }
        return data;
    }

    /** 插入或更新玩家数据（以 user_id 为唯一键） */
    public void insertOrUpdate(PlayerData data) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("user_id", data.getUserId());
        values.put("coins", data.getCoins());
        values.put("unlocked_pro", data.isUnlockedPro() ? 1 : 0);
        values.put("unlocked_promax", data.isUnlockedPromax() ? 1 : 0);

        // 使用 replace 实现插入或更新
        db.replace(ScoreDatabaseHelper.TABLE_PLAYER_DATA, null, values);
    }

    /** 增加代币 */
    public void addCoins(int userId, int amountToAdd) {
        PlayerData data = getOrCreatePlayerData(userId);
        data.setCoins(data.getCoins() + amountToAdd);
        insertOrUpdate(data);
    }

    /** 扣除代币（购买用），返回是否成功 */
    public boolean deductCoins(int userId, int cost) {
        PlayerData data = getOrCreatePlayerData(userId);
        if (data.getCoins() >= cost) {
            data.setCoins(data.getCoins() - cost);
            insertOrUpdate(data);
            return true;
        }
        return false;
    }

    /** 解锁 PRO 飞机 */
    public void unlockPro(int userId) {
        PlayerData data = getOrCreatePlayerData(userId);
        data.setUnlockedPro(true);
        insertOrUpdate(data);
    }

    /** 解锁 PROMAX 飞机 */
    public void unlockPromax(int userId) {
        PlayerData data = getOrCreatePlayerData(userId);
        data.setUnlockedPromax(true);
        insertOrUpdate(data);
    }

    private PlayerData cursorToPlayerData(Cursor cursor) {
        PlayerData data = new PlayerData();
        data.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
        data.setUserId(cursor.getInt(cursor.getColumnIndexOrThrow("user_id")));
        data.setCoins(cursor.getInt(cursor.getColumnIndexOrThrow("coins")));
        data.setUnlockedPro(cursor.getInt(cursor.getColumnIndexOrThrow("unlocked_pro")) == 1);
        data.setUnlockedPromax(cursor.getInt(cursor.getColumnIndexOrThrow("unlocked_promax")) == 1);
        return data;
    }
}
