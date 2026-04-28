package com.game.myserver.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class PlayerSession {
    private final String playerId;          // 连接会话ID（如 Player_1）
    private String playerName;
    private int userId = -1;                 // ★ 新增：数据库用户ID（未登录时为-1）
    private final Socket socket;
    private final BufferedReader reader;
    private final PrintWriter writer;

    // Game state
    private int currentScore = 0;
    private int finalScore = 0;
    private boolean isAlive = true;
    private long lastHeartbeatTime;
    private int coins = 0;

    private boolean unlockedPro = false;    // PRO飞机是否解锁
    private boolean unlockedPromax = false; // PROMAX飞机是否解锁

    // 新增字段
    private String selectedDifficulty = null;  // 选择的难度
    private String selectedAircraft = null;   // 选择的飞机
    private boolean difficultyConfirmed = false;  // 难度是否已确认
    private boolean aircraftConfirmed = false;     // 飞机是否已确认

    public PlayerSession(String playerId, Socket socket) throws IOException {
        this.playerId = playerId;
        this.socket = socket;
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.writer = new PrintWriter(socket.getOutputStream(), true);
        this.lastHeartbeatTime = System.currentTimeMillis();
    }

    // Getter / Setter

    // 在其他 getter/setter 附近添加
    public int getCoins() { return coins; }
    public void setCoins(int coins) { this.coins = coins; }

    public boolean isUnlockedPro() { return unlockedPro; }
    public void setUnlockedPro(boolean unlocked) { this.unlockedPro = unlocked; }

    public boolean isUnlockedPromax() { return unlockedPromax; }
    public void setUnlockedPromax(boolean unlocked) { this.unlockedPromax = unlocked; }

    public String getSelectedDifficulty() { return selectedDifficulty; }
    public void setSelectedDifficulty(String difficulty) { this.selectedDifficulty = difficulty; }

    public String getSelectedAircraft() { return selectedAircraft; }
    public void setSelectedAircraft(String aircraft) { this.selectedAircraft = aircraft; }

    public boolean isDifficultyConfirmed() { return difficultyConfirmed; }
    public void setDifficultyConfirmed(boolean confirmed) { this.difficultyConfirmed = confirmed; }

    public boolean isAircraftConfirmed() { return aircraftConfirmed; }
    public void setAircraftConfirmed(boolean confirmed) { this.aircraftConfirmed = confirmed; }

    public String getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String name) { this.playerName = name; }

    /** ★ 新增：获取数据库用户ID */
    public int getUserId() { return userId; }

    /** ★ 新增：设置数据库用户ID（登录成功后调用） */
    public void setUserId(int id) { this.userId = id; }

    /** ★ 新增：是否已登录 */
    public boolean isLoggedIn() { return userId > 0; }

    /** Get player name, or ID if name not set */
    public String getPlayerNameOrId() {
        return (playerName != null && !playerName.isEmpty()) ? playerName : playerId;
    }

    public int getCurrentScore() { return currentScore; }
    public void setCurrentScore(int score) { this.currentScore = score; }

    public int getFinalScore() { return finalScore; }
    public void setFinalScore(int score) { this.finalScore = score; }

    public boolean isAlive() { return isAlive; }
    public void setDead() { this.isAlive = false; }

    /** 重置游戏状态（用于开始新游戏或离开房间后） */
    public void resetForNewGame() {
        this.currentScore = 0;
        this.finalScore = 0;
        this.isAlive = true;
        this.selectedDifficulty = null;
        this.selectedAircraft = null;
        this.difficultyConfirmed = false;
        this.aircraftConfirmed = false;
    }

    public long getLastHeartbeatTime() { return lastHeartbeatTime; }
    public void updateHeartbeat() { this.lastHeartbeatTime = System.currentTimeMillis(); }

    // Network communication methods

    /** Send a message to this client */
    public void sendMessage(String message) {
        if (writer != null && !socket.isClosed()) {
            writer.println(message);
        }
    }

    /** Receive a message (blocking, must be called in child thread) */
    public String receiveMessage() throws IOException {
        return reader.readLine();
    }

    /** Close socket connection */
    public void close() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    /** Check if connection is closed */
    public boolean isClosed() {
        return socket == null || socket.isClosed();
    }
}
