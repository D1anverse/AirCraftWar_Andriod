package edu.hitsz.entity;

/**
 * 玩家本地数据实体类 - 存储在 SQLite player_data 表中
 * 用于离线时代币和飞机解锁状态的本地缓存
 */
public class PlayerData {
    private int id;
    private int userId;           // 对应服务器的用户ID
    private int coins;            // 代币数量
    private boolean unlockedPro;   // PRO飞机是否解锁
    private boolean unlockedPromax; // PROMAX飞机是否解锁

    public PlayerData() {}

    public PlayerData(int userId, int coins, boolean unlockedPro, boolean unlockedPromax) {
        this.userId = userId;
        this.coins = coins;
        this.unlockedPro = unlockedPro;
        this.unlockedPromax = unlockedPromax;
    }

    // Getter & Setter
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getCoins() { return coins; }
    public void setCoins(int coins) { this.coins = coins; }

    public boolean isUnlockedPro() { return unlockedPro; }
    public void setUnlockedPro(boolean unlockedPro) { this.unlockedPro = unlockedPro; }

    public boolean isUnlockedPromax() { return unlockedPromax; }
    public void setUnlockedPromax(boolean unlockedPromax) { this.unlockedPromax = unlockedPromax; }

    @Override
    public String toString() {
        return "PlayerData{userId=" + userId + ", coins=" + coins
                + ", pro=" + unlockedPro + ", promax=" + unlockedPromax + "}";
    }
}
