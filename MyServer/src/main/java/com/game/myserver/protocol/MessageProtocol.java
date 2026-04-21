package com.game.myserver.protocol;

public class MessageProtocol {
    // ===== 客户端 → 服务器 =====
    public static final String JOIN_ROOM = "JOIN_ROOM";        // 加入房间  JOIN_ROOM|昵称
    public static final String SCORE_UPDATE = "SCORE_UPDATE";  // 分数更新  SCORE_UPDATE|分数
    public static final String GAME_OVER = "GAME_OVER";        // 游戏结束  GAME_OVER|最终分
    public static final String HEARTBEAT = "HEARTBEAT";        // 心跳保活
    public static final String LEAVE_ROOM = "LEAVE_ROOM";      // 离开房间

    // ===== 服务器 → 客户端 =====
    public static final String OPPONENT_JOINED = "OPPONENT_JOINED";   // 对手加入
    public static final String OPPONENT_SCORE = "OPPONENT_SCORE";     // 对手分数更新
    public static final String OPPONENT_DEAD = "OPPONENT_DEAD";       // 对手死亡
    public static final String MATCH_END = "MATCH_END";               // 对战结束
    public static final String WAITING_OPPONENT = "WAITING_OPPONENT"; // 等待对手
    public static final String ERROR = "ERROR";                       // 错误信息
    public static final String ROOM_FULL = "ROOM_FULL";               // 房间已满

    /**
     * 解析消息: "TYPE|body" → ["TYPE", "body"]
     */
    public static String[] parseMessage(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        return raw.split("\\|", 2);
    }

    /**
     * 构建消息: buildMessage("A","B") → "A|B"
     */
    public static String buildMessage(String type, String body) {
        return type + "|" + body;
    }
}
