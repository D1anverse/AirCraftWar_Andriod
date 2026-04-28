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

    public static final String REGISTER = "REGISTER";           // 注册  REGISTER|nick,email,pwd
    public static final String LOGIN = "LOGIN";                 // 登录  LOGIN|nick,pwd
    public static final String SEARCH_USER = "SEARCH_USER";     // 搜索用户  SEARCH_USER|keyword
    public static final String ADD_FRIEND = "ADD_FRIEND";       // 添加好友  ADD_FRIEND|myId,friendId
    public static final String GET_FRIENDS = "GET_FRIENDS";     // 获取好友列表  GET_FRIENDS|userId
    public static final String INVITE_FRIEND = "INVITE_FRIEND"; // 邀请对战  INVITE_FRIEND|fromId,toId
    public static final String BUY_AIRCRAFT = "BUY_AIRCRAFT";   // 购买战机  BUY_AIRCRAFT|userId,type
    public static final String SYNC_COINS = "SYNC_COINS";       // 同步代币  SYNC_COINS|userId,amount
    public static final String QUERY_COINS = "QUERY_COINS";     // 查询代币  QUERY_COINS|userId
    public static final String COINS_INFO = "COINS_INFO";      // COINS_INFO|userId,coins,pro,promax
    public static final String ACCEPT_FRIEND = "ACCEPT_FRIEND";       // 接受好友
    public static final String REJECT_FRIEND = "REJECT_FRIEND";       // 拒绝好友
    public static final String ACCEPT_INVITE = "ACCEPT_INVITE";       // 接受对战邀请
    public static final String REJECT_INVITE = "REJECT_INVITE";        // 拒绝对战邀请
    public static final String FRIEND_REQUEST = "FRIEND_REQUEST";      // 好友申请通知
    public static final String FRIEND_ACCEPTED = "FRIEND_ACCEPTED";   // 好友申请被接受
    public static final String INVITE_REQUEST = "INVITE_REQUEST";      // 邀请通知
    public static final String INVITE_ACCEPTED = "INVITE_ACCEPTED";   // 邀请被接受

    public static final String DIFFICULTY_SELECT = "DIFFICULTY_SELECT";     // 难度选择
    public static final String OPPONENT_DIFFICULTY = "OPPONENT_DIFFICULTY"; // 对手选择了难度
    public static final String DIFFICULTY_MATCHED = "DIFFICULTY_MATCHED";   // 难度匹配成功
    public static final String DIFFICULTY_RETRY = "DIFFICULTY_RETRY";       // 难度不匹配，需要重选
    public static final String AIRCRAFT_SELECT = "AIRCRAFT_SELECT";         // 飞机选择
    public static final String OPPONENT_AIRCRAFT_SELECTED = "OPPONENT_AIRCRAFT_SELECTED"; // 对手选择了飞机
    public static final String READY_TO_FIGHT = "READY_TO_FIGHT";           // 双方确认，可以开战
    public static final String OPPONENT_LEFT = "OPPONENT_LEFT";             // 对手离开了
    public static final String ROOM_CANCELLED = "ROOM_CANCELLED";           // 房间已取消

    // 服务器 → 客户端（响应）
    public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String LOGIN_ERROR = "LOGIN_ERROR";
    public static final String REGISTER_SUCCESS = "REGISTER_SUCCESS";
    public static final String REGISTER_ERROR = "REGISTER_ERROR";
    public static final String SEARCH_RESULT = "SEARCH_RESULT";
    public static final String FRIEND_LIST = "FRIEND_LIST";
    public static final String ADD_FRIEND_OK = "ADD_FRIEND_OK";
    public static final String ADD_FRIEND_FAIL = "ADD_FRIEND_FAIL";
    public static final String INVITE_SENT = "INVITE_SENT";
    public static final String INVITE_FAIL = "INVITE_FAIL";
    public static final String BUY_OK = "BUY_OK";
    public static final String BUY_FAIL = "BUY_FAIL";
    public static final String SYNC_OK = "SYNC_OK";
    public static final String SYNC_FAIL = "SYNC_FAIL";

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
