package com.game.myserver;

import com.game.myserver.db.DatabaseManager;
import com.game.myserver.model.BattleRoom;
import com.game.myserver.model.PlayerSession;
import com.game.myserver.protocol.MessageProtocol;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class MyClass {

    private static final int PORT = 8080;

    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private volatile boolean running = true;

    private final List<BattleRoom> waitingRooms = new CopyOnWriteArrayList<>();
    private final List<BattleRoom> activeRooms = new CopyOnWriteArrayList<>();
    private final AtomicInteger playerIdCounter = new AtomicInteger(0);

    private final Map<Integer, ClientHandler> onlineUsers = new ConcurrentHashMap<>();

    private DatabaseManager db;

    public static void main(String[] args) {
        new MyClass().start();
    }

    public void start() {
        try {
            db = DatabaseManager.getInstance();
            serverSocket = new ServerSocket(PORT);
            threadPool = Executors.newCachedThreadPool();
            printStartupInfo();

            while (running) {
                Socket clientSocket = serverSocket.accept();
                String playerId = "Player_" + playerIdCounter.incrementAndGet();
                System.out.println("\n新连接 -> " + playerId);
                threadPool.execute(new ClientHandler(clientSocket, playerId));
            }

        } catch (IOException e) {
            if (running) {
                System.err.println("Server error: " + e.getMessage());
                e.printStackTrace();
            }
        } finally {
            stop();
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) { e.printStackTrace(); }
        if (threadPool != null) threadPool.shutdownNow();
        System.out.println("\nServer stopped");
    }

    private void printStartupInfo() {
        System.out.println("");
        System.out.println("========================================");
        System.out.println("||                                      ||");
        System.out.println("||    AirWar Battle Server v2.0         ||");
        System.out.println("||  + 登录注册 | + 好友 | + 商店         ||");
        System.out.println("||                                      ||");
        System.out.println("========================================");
        System.out.println("");
        System.out.println("[OK] Server started on port: " + PORT);
        System.out.println("[OK] Database ready: game_server.db");
        System.out.println("[..] Waiting for players...");
        System.out.println("----------------------------------------");
    }

    private class ClientHandler implements Runnable {

        private final Socket socket;
        private final PlayerSession player;
        private BattleRoom currentRoom;

        public ClientHandler(Socket socket, String playerId) throws IOException {
            this.socket = socket;
            this.player = new PlayerSession(playerId, socket);
        }

        @Override
        public void run() {
            try {
                String message;
                while ((message = player.receiveMessage()) != null) {
                    processMessage(message);
                }
            } catch (IOException e) {
                System.out.println("断开 -> " + player.getPlayerNameOrId()
                        + " (" + e.getMessage() + ")");
            } finally {
                onDisconnect();
            }
        }

        private void processMessage(String rawMessage) {
            System.out.println("[RECV] " + rawMessage);

            String[] parts = MessageProtocol.parseMessage(rawMessage);
            if (parts == null || parts.length < 2) return;

            String type = parts[0];
            String body = parts[1];

            switch (type) {
                case MessageProtocol.JOIN_ROOM:
                    handleJoinRoom(body);
                    break;
                case MessageProtocol.SCORE_UPDATE:
                    handleScoreUpdate(body);
                    break;
                case MessageProtocol.GAME_OVER:
                    handleGameOver(body);
                    break;
                case MessageProtocol.HEARTBEAT:
                    player.updateHeartbeat();
                    break;
                case MessageProtocol.LEAVE_ROOM:
                    handleLeaveRoom();
                    break;
                case MessageProtocol.DIFFICULTY_SELECT:
                    handleDifficultySelect(body);
                    break;
                case MessageProtocol.AIRCRAFT_SELECT:
                    handleAircraftSelect(body);
                    break;
                case MessageProtocol.REGISTER:
                    handleRegister(body);
                    break;
                case MessageProtocol.LOGIN:
                    handleLogin(body);
                    break;
                case MessageProtocol.SEARCH_USER:
                    handleSearchUser(body);
                    break;
                case MessageProtocol.ADD_FRIEND:
                    handleAddFriend(body);
                    break;
                case MessageProtocol.GET_FRIENDS:
                    handleGetFriends(body);
                    break;
                case MessageProtocol.INVITE_FRIEND:
                    handleInviteFriend(body);
                    break;
                case MessageProtocol.BUY_AIRCRAFT:
                    handleBuyAircraft(body);
                    break;
                case MessageProtocol.SYNC_COINS:
                    handleSyncCoins(body);
                    break;
                case MessageProtocol.QUERY_COINS:
                    handleQueryCoins(body);
                    break;
                case MessageProtocol.ACCEPT_INVITE:
                    handleAcceptInvite(body);
                    break;
                case MessageProtocol.REJECT_INVITE:
                    handleRejectInvite(body);
                    break;
                case MessageProtocol.ACCEPT_FRIEND:
                    handleAcceptFriend(body);
                    break;
                case MessageProtocol.REJECT_FRIEND:
                    handleRejectFriend(body);
                    break;
                default:
                    System.out.println("Unknown command: " + type);
            }
        }

        private void handleRegister(String body) {
            String[] params = body.split(",");
            if (params.length < 3) {
                player.sendMessage(MessageProtocol.buildMessage(
                        MessageProtocol.REGISTER_ERROR, "参数不完整"));
                return;
            }

            String nickname = params[0].trim();
            String email = params[1].trim();
            String password = params[2].trim();

            Connection conn = null;
            PreparedStatement checkStmt = null;
            PreparedStatement insertStmt = null;
            ResultSet rs = null;

            try {
                conn = db.getConnection();

                checkStmt = conn.prepareStatement(
                        "SELECT id FROM users WHERE nickname = ?");
                checkStmt.setString(1, nickname);
                rs = checkStmt.executeQuery();
                if (rs.next()) {
                    player.sendMessage(MessageProtocol.buildMessage(
                            MessageProtocol.REGISTER_ERROR, "该昵称已被注册"));
                    return;
                }
                DatabaseManager.closeQuietly(rs, checkStmt);

                checkStmt = conn.prepareStatement(
                        "SELECT id FROM users WHERE email = ?");
                checkStmt.setString(1, email);
                rs = checkStmt.executeQuery();
                if (rs.next()) {
                    player.sendMessage(MessageProtocol.buildMessage(
                            MessageProtocol.REGISTER_ERROR, "该邮箱已被注册"));
                    return;
                }
                DatabaseManager.closeQuietly(rs, checkStmt);

                insertStmt = conn.prepareStatement(
                        "INSERT INTO users(nickname, email, password, coins, unlocked_pro, unlocked_promax) "
                                + "VALUES(?,?,?,?,?,?)");
                insertStmt.setString(1, nickname);
                insertStmt.setString(2, email);
                insertStmt.setString(3, password);
                insertStmt.setInt(4, 0);
                insertStmt.setInt(5, 0);
                insertStmt.setInt(6, 0);
                insertStmt.executeUpdate();
                DatabaseManager.closeQuietly(insertStmt);

                Statement idStmt = conn.createStatement();
                ResultSet genKeys = idStmt.executeQuery("SELECT last_insert_rowid()");
                int newUserId = -1;
                if (genKeys.next()) {
                    newUserId = genKeys.getInt(1);
                }
                DatabaseManager.closeQuietly(genKeys, idStmt);

                player.setUserId(newUserId);
                player.setPlayerName(nickname);
                onlineUsers.put(newUserId, this);

                System.out.println("[REG] 新用户注册成功: " + nickname + " (id=" + newUserId + ")");

                player.sendMessage(MessageProtocol.buildMessage(
                        MessageProtocol.REGISTER_SUCCESS,
                        newUserId + "," + nickname + "," + email));

            } catch (SQLException e) {
                e.printStackTrace();
                player.sendMessage(MessageProtocol.buildMessage(
                        MessageProtocol.REGISTER_ERROR, "服务器内部错误"));
            } finally {
                DatabaseManager.closeQuietly(rs, checkStmt, insertStmt);
            }
        }

        private void handleLogin(String body) {
            String[] params = body.split(",");
            if (params.length < 2) {
                player.sendMessage(MessageProtocol.buildMessage(
                        MessageProtocol.LOGIN_ERROR, "参数不完整"));
                return;
            }

            String nickname = params[0].trim();
            String password = params[1].trim();

            Connection conn = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;

            try {
                conn = db.getConnection();
                stmt = conn.prepareStatement(
                        "SELECT id, nickname, email, coins, unlocked_pro, unlocked_promax "
                                + "FROM users WHERE nickname = ? AND password = ?");
                stmt.setString(1, nickname);
                stmt.setString(2, password);
                rs = stmt.executeQuery();

                if (rs.next()) {
                    int userId = rs.getInt("id");
                    String nick = rs.getString("nickname");
                    String email = rs.getString("email");
                    int coins = rs.getInt("coins");
                    int pro = rs.getInt("unlocked_pro");
                    int promax = rs.getInt("unlocked_promax");

                    player.setUserId(userId);
                    player.setPlayerName(nick);
                    player.setCoins(coins);
                    player.setUnlockedPro(pro == 1);
                    player.setUnlockedPromax(promax == 1);
                    onlineUsers.put(userId, this);

                    System.out.println("[LOGIN] " + nick + " (id=" + userId + ") 登录成功");

                    player.sendMessage(MessageProtocol.buildMessage(
                            MessageProtocol.LOGIN_SUCCESS,
                            userId + "," + nick + "," + email + ","
                                    + coins + "," + pro + "," + promax));
                } else {
                    player.sendMessage(MessageProtocol.buildMessage(
                            MessageProtocol.LOGIN_ERROR, "账号或密码错误"));
                }

            } catch (SQLException e) {
                e.printStackTrace();
                player.sendMessage(MessageProtocol.buildMessage(
                        MessageProtocol.LOGIN_ERROR, "服务器内部错误"));
            } finally {
                DatabaseManager.closeQuietly(rs, stmt);
            }
        }

        private void handleDifficultySelect(String difficulty) {
            if (!difficulty.equals("easy") && !difficulty.equals("normal") && !difficulty.equals("hard")) {
                player.sendMessage(MessageProtocol.buildMessage(
                        MessageProtocol.ERROR, "无效的难度值"));
                return;
            }

            if (currentRoom == null) {
                player.sendMessage(MessageProtocol.buildMessage(
                        MessageProtocol.ERROR, "不在房间内"));
                return;
            }

            currentRoom.selectDifficulty(player, difficulty);
        }

        private void handleAircraftSelect(String body) {
            String aircraftType = body.trim();

            if (!aircraftType.equals("hero") && !aircraftType.equals("hero_pro")
                    && !aircraftType.equals("hero_promax")) {
                aircraftType = "hero";
            }

            if (currentRoom == null) {
                player.sendMessage(MessageProtocol.buildMessage(
                        MessageProtocol.ERROR, "不在房间内"));
                return;
            }

            currentRoom.selectAircraft(player, aircraftType);
        }

        private void handleSearchUser(String keyword) {
            Connection conn = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;

            try {
                conn = db.getConnection();
                stmt = conn.prepareStatement(
                        "SELECT id, nickname FROM users WHERE nickname LIKE ? LIMIT 10");
                stmt.setString(1, "%" + keyword.trim() + "%");
                rs = stmt.executeQuery();

                StringBuilder result = new StringBuilder();
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    int uid = rs.getInt("id");
                    String status = onlineUsers.containsKey(uid) ? "online" : "offline";
                    result.append(uid).append(",").append(rs.getString("nickname")).append(",").append(status).append("\n");
                }

                if (found) {
                    player.sendMessage(MessageProtocol.buildMessage(
                            MessageProtocol.SEARCH_RESULT, result.toString().trim()));
                } else {
                    player.sendMessage(MessageProtocol.buildMessage(
                            MessageProtocol.SEARCH_RESULT, "NOT_FOUND"));
                }

            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                DatabaseManager.closeQuietly(rs, stmt);
            }
        }

        private void handleAddFriend(String body) {
            String[] ids = body.split(",");
            if (ids.length < 2) return;

            int myId, targetId;
            try {
                myId = Integer.parseInt(ids[0].trim());
                targetId = Integer.parseInt(ids[1].trim());
            } catch (NumberFormatException e) {
                player.sendMessage(MessageProtocol.buildMessage(
                        MessageProtocol.ADD_FRIEND_FAIL, "参数错误"));
                return;
            }

            if (myId == targetId) {
                player.sendMessage(MessageProtocol.buildMessage(
                        MessageProtocol.ADD_FRIEND_FAIL, "不能添加自己为好友"));
                return;
            }

            Connection conn = null;
            PreparedStatement checkStmt = null;
            PreparedStatement insertStmt = null;
            ResultSet rs = null;

            try {
                conn = db.getConnection();

                checkStmt = conn.prepareStatement(
                        "SELECT id, status FROM friends WHERE "
                                + "(user_id=? AND friend_id=?) OR (user_id=? AND friend_id=?)");
                checkStmt.setInt(1, myId);
                checkStmt.setInt(2, targetId);
                checkStmt.setInt(3, targetId);
                checkStmt.setInt(4, myId);
                rs = checkStmt.executeQuery();
                if (rs.next()) {
                    String status = rs.getString("status");
                    if ("accepted".equals(status)) {
                        player.sendMessage(MessageProtocol.buildMessage(
                                MessageProtocol.ADD_FRIEND_FAIL, "你们已经是好友了"));
                    } else if ("pending".equals(status)) {
                        player.sendMessage(MessageProtocol.buildMessage(
                                MessageProtocol.ADD_FRIEND_FAIL, "已发送过请求，请等待对方处理"));
                    }
                    return;
                }
                DatabaseManager.closeQuietly(rs, checkStmt);

                checkStmt = conn.prepareStatement("SELECT id, nickname FROM users WHERE id=?");
                checkStmt.setInt(1, targetId);
                rs = checkStmt.executeQuery();
                if (!rs.next()) {
                    player.sendMessage(MessageProtocol.buildMessage(
                            MessageProtocol.ADD_FRIEND_FAIL, "目标用户不存在"));
                    return;
                }
                String targetNickname = rs.getString("nickname");
                DatabaseManager.closeQuietly(rs, checkStmt);

                String myNickname = player.getPlayerName();
                if (myNickname == null) {
                    PreparedStatement nickStmt = conn.prepareStatement(
                            "SELECT nickname FROM users WHERE id=?");
                    nickStmt.setInt(1, myId);
                    ResultSet nickRs = nickStmt.executeQuery();
                    if (nickRs.next()) {
                        myNickname = nickRs.getString("nickname");
                    }
                    DatabaseManager.closeQuietly(nickRs, nickStmt);
                }

                insertStmt = conn.prepareStatement(
                        "INSERT INTO friends(user_id, friend_id, status) VALUES(?,?,'pending')");
                insertStmt.setInt(1, myId);
                insertStmt.setInt(2, targetId);
                insertStmt.executeUpdate();

                System.out.println("[FRIEND] 用户" + myId + "(" + myNickname + ") 向 " + targetId + "(" + targetNickname + ") 发送好友请求");
                player.sendMessage(MessageProtocol.buildMessage(
                        MessageProtocol.ADD_FRIEND_OK, "请求已发送"));

                ClientHandler targetHandler = onlineUsers.get(targetId);
                if (targetHandler != null) {
                    targetHandler.player.sendMessage(MessageProtocol.buildMessage(
                            "FRIEND_REQUEST", myId + "," + myNickname));
                    System.out.println("[FRIEND] 已通知 " + targetNickname + " 有新的好友请求");
                }

            } catch (SQLException e) {
                e.printStackTrace();
                player.sendMessage(MessageProtocol.buildMessage(
                        MessageProtocol.ADD_FRIEND_FAIL, "服务器错误"));
            } finally {
                DatabaseManager.closeQuietly(rs, checkStmt, insertStmt);
            }
        }

        private void handleAcceptInvite(String body) {
            String[] ids = body.split(",");
            if (ids.length < 2) return;

            int myId, fromId;
            try {
                myId = Integer.parseInt(ids[0].trim());
                fromId = Integer.parseInt(ids[1].trim());
            } catch (NumberFormatException e) {
                return;
            }

            String myNickname = player.getPlayerName();

            ClientHandler inviter = onlineUsers.get(fromId);
            if (inviter != null) {
                inviter.player.sendMessage(MessageProtocol.buildMessage(
                        "INVITE_ACCEPTED", myNickname + "接受了您的邀请"));
            }

            player.sendMessage(MessageProtocol.buildMessage(
                    "INVITE_CONFIRMED", "已接受邀请"));

            System.out.println("[INVITE] 用户" + myId + " 接受了 " + fromId + " 的邀请");
        }

        private void handleRejectInvite(String body) {
            String[] ids = body.split(",");
            if (ids.length < 2) return;

            int myId, fromId;
            try {
                myId = Integer.parseInt(ids[0].trim());
                fromId = Integer.parseInt(ids[1].trim());
            } catch (NumberFormatException e) {
                return;
            }

            String myNickname = player.getPlayerName();

            ClientHandler inviter = onlineUsers.get(fromId);
            if (inviter != null) {
                inviter.player.sendMessage(MessageProtocol.buildMessage(
                        "INVITE_REJECTED", myNickname));
            }
        }

        private void handleAcceptFriend(String body) {
            String[] ids = body.split(",");
            if (ids.length < 2) return;

            int myId, fromId;
            try {
                myId = Integer.parseInt(ids[0].trim());
                fromId = Integer.parseInt(ids[1].trim());
            } catch (NumberFormatException e) {
                return;
            }

            String myNickname = player.getPlayerName();

            Connection conn = null;
            PreparedStatement stmt = null;

            try {
                conn = db.getConnection();

                stmt = conn.prepareStatement(
                        "UPDATE friends SET status='accepted' WHERE user_id=? AND friend_id=? AND status='pending'");
                stmt.setInt(1, fromId);
                stmt.setInt(2, myId);
                stmt.executeUpdate();
                DatabaseManager.closeQuietly(stmt);

                stmt = conn.prepareStatement(
                        "INSERT INTO friends(user_id, friend_id, status) VALUES(?,?,'accepted')");
                stmt.setInt(1, myId);
                stmt.setInt(2, fromId);
                stmt.executeUpdate();

                System.out.println("[FRIEND] 用户" + myId + " 同意了 " + fromId + " 的好友请求");

                ClientHandler target = onlineUsers.get(fromId);
                if (target != null) {
                    target.player.sendMessage(MessageProtocol.buildMessage(
                            "FRIEND_ACCEPTED", myNickname));
                }

            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                DatabaseManager.closeQuietly(stmt);
            }
        }

        private void handleRejectFriend(String body) {
            String[] ids = body.split(",");
            if (ids.length < 2) return;

            int myId, fromId;
            try {
                myId = Integer.parseInt(ids[0].trim());
                fromId = Integer.parseInt(ids[1].trim());
            } catch (NumberFormatException e) {
                return;
            }

            Connection conn = null;
            PreparedStatement stmt = null;

            try {
                conn = db.getConnection();

                stmt = conn.prepareStatement(
                        "DELETE FROM friends WHERE user_id=? AND friend_id=? AND status='pending'");
                stmt.setInt(1, fromId);
                stmt.setInt(2, myId);
                int deleted = stmt.executeUpdate();

                if (deleted > 0) {
                    System.out.println("[FRIEND] 用户" + myId + " 拒绝了 " + fromId + " 的好友请求");

                    String myNickname = player.getPlayerName();
                    ClientHandler requester = onlineUsers.get(fromId);
                    if (requester != null && myNickname != null) {
                        requester.player.sendMessage(MessageProtocol.buildMessage(
                                "FRIEND_REJECTED", myNickname));
                    }
                }

            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                DatabaseManager.closeQuietly(stmt);
            }
        }

        private void handleGetFriends(String userIdStr) {
            int userId;
            try {
                userId = Integer.parseInt(userIdStr.trim());
            } catch (NumberFormatException e) {
                return;
            }

            Connection conn = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;

            try {
                conn = db.getConnection();
                stmt = conn.prepareStatement(
                        "SELECT DISTINCT u.id, u.nickname FROM users u "
                                + "INNER JOIN friends f ON (f.friend_id = u.id OR f.user_id = u.id) "
                                + "WHERE (f.user_id = ? OR f.friend_id = ?) AND f.status='accepted' AND u.id != ?");
                stmt.setInt(1, userId);
                stmt.setInt(2, userId);
                stmt.setInt(3, userId);
                rs = stmt.executeQuery();

                StringBuilder list = new StringBuilder();
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    int uid = rs.getInt("id");
                    System.out.println("[DEBUG] 第X个好友: uid=" + uid + ", name=" + rs.getString("nickname"));
                    String status = onlineUsers.containsKey(uid) ? "online" : "offline";
                    list.append(uid).append(",").append(rs.getString("nickname")).append(",").append(status).append(" ");
                }

                if (found) {
                    player.sendMessage(MessageProtocol.buildMessage(
                            MessageProtocol.FRIEND_LIST, list.toString().trim()));
                } else {
                    player.sendMessage(MessageProtocol.buildMessage(
                            MessageProtocol.FRIEND_LIST, "NONE"));
                }

            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                DatabaseManager.closeQuietly(rs, stmt);
            }
        }

        private void handleInviteFriend(String body) {
            String[] ids = body.split(",");
            if (ids.length < 2) return;

            int fromId, toId;
            try {
                fromId = Integer.parseInt(ids[0].trim());
                toId = Integer.parseInt(ids[1].trim());
            } catch (NumberFormatException e) {
                player.sendMessage(MessageProtocol.buildMessage(
                        MessageProtocol.INVITE_FAIL, "参数错误"));
                return;
            }

            String fromNickname = player.getPlayerName();
            if (fromNickname == null) {
                fromNickname = "玩家" + fromId;
            }

            ClientHandler targetHandler = onlineUsers.get(toId);
            if (targetHandler != null) {
                targetHandler.player.sendMessage(MessageProtocol.buildMessage(
                        "INVITE_REQUEST", fromId + "," + fromNickname));
                player.sendMessage(MessageProtocol.buildMessage(
                        MessageProtocol.INVITE_SENT, "邀请已发送"));
            } else {
                player.sendMessage(MessageProtocol.buildMessage(
                        MessageProtocol.INVITE_FAIL, "对方不在线"));
            }

            System.out.println("[INVITE] 用户" + fromId + " 邀请 " + toId + " 对战");
        }

        private void handleBuyAircraft(String body) {
            String[] params = body.split(",", 2);
            if (params.length < 2) {
                player.sendMessage(MessageProtocol.buildMessage(
                        MessageProtocol.BUY_FAIL, "参数错误"));
                return;
            }

            int userId;
            try {
                userId = Integer.parseInt(params[0].trim());
            } catch (NumberFormatException e) {
                player.sendMessage(MessageProtocol.buildMessage(
                        MessageProtocol.BUY_FAIL, "参数错误"));
                return;
            }

            String aircraftType = params[1].trim().toUpperCase();
            int cost;
            String column;

            if ("PRO".equals(aircraftType)) {
                cost = 5000;
                column = "unlocked_pro";
            } else if ("PROMAX".equals(aircraftType)) {
                cost = 15000;
                column = "unlocked_promax";
            } else {
                player.sendMessage(MessageProtocol.buildMessage(
                        MessageProtocol.BUY_FAIL, "未知机型: " + aircraftType));
                return;
            }

            Connection conn = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;

            try {
                conn = db.getConnection();
                conn.setAutoCommit(false);

                stmt = conn.prepareStatement(
                        "SELECT coins, " + column + " FROM users WHERE id = ?");
                stmt.setInt(1, userId);
                rs = stmt.executeQuery();

                if (!rs.next()) {
                    conn.rollback();
                    player.sendMessage(MessageProtocol.buildMessage(
                            MessageProtocol.BUY_FAIL, "用户不存在"));
                    return;
                }

                int coins = rs.getInt("coins");
                int alreadyUnlocked = rs.getInt(column);
                DatabaseManager.closeQuietly(rs, stmt);

                if (alreadyUnlocked == 1) {
                    conn.rollback();
                    player.sendMessage(MessageProtocol.buildMessage(
                            MessageProtocol.BUY_FAIL, "已经解锁过了"));
                    return;
                }

                if (coins < cost) {
                    conn.rollback();
                    player.sendMessage(MessageProtocol.buildMessage(
                            MessageProtocol.BUY_FAIL, "代币不足！需要" + cost + "，当前" + coins));
                    return;
                }

                stmt = conn.prepareStatement(
                        "UPDATE users SET coins = coins - ?, " + column + " = 1 WHERE id = ?");
                stmt.setInt(1, cost);
                stmt.setInt(2, userId);
                stmt.executeUpdate();

                stmt = conn.prepareStatement(
                        "SELECT coins, unlocked_pro, unlocked_promax FROM users WHERE id = ?");
                stmt.setInt(1, userId);
                rs = stmt.executeQuery();

                int newCoins = 0;
                int newPro = 0;
                int newPromax = 0;
                if (rs.next()) {
                    newCoins = rs.getInt("coins");
                    newPro = rs.getInt("unlocked_pro");
                    newPromax = rs.getInt("unlocked_promax");
                }

                conn.commit();

                System.out.println("[SHOP] 用户" + userId + " 购买 " + aircraftType
                        + " 花费" + cost + " 代币, 剩余" + newCoins);

                player.setCoins(newCoins);
                if ("PRO".equals(aircraftType)) {
                    player.setUnlockedPro(true);
                } else if ("PROMAX".equals(aircraftType)) {
                    player.setUnlockedPromax(true);
                }

                player.sendMessage(MessageProtocol.BUY_OK + "|" + newCoins + "," + newPro + "," + newPromax);

            } catch (SQLException e) {
                try { if (conn != null) conn.rollback(); } catch (SQLException ignored) {}
                e.printStackTrace();
                player.sendMessage(MessageProtocol.buildMessage(
                        MessageProtocol.BUY_FAIL, "交易失败"));
            } finally {
                try { if (conn != null) conn.setAutoCommit(true); } catch (SQLException ignored) {}
                DatabaseManager.closeQuietly(rs, stmt);
            }
        }

        private void handleSyncCoins(String body) {
            String[] params = body.split(",");
            if (params.length < 2) return;

            int userId, amount;
            try {
                userId = Integer.parseInt(params[0].trim());
                amount = Integer.parseInt(params[1].trim());
            } catch (NumberFormatException e) {
                return;
            }

            Connection conn = null;
            PreparedStatement stmt = null;

            try {
                conn = db.getConnection();
                stmt = conn.prepareStatement(
                        "UPDATE users SET coins = coins + ? WHERE id = ?");
                stmt.setInt(1, amount);
                stmt.setInt(2, userId);
                stmt.executeUpdate();

                stmt = conn.prepareStatement("SELECT coins FROM users WHERE id = ?");
                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();
                int total = 0;
                if (rs.next()) total = rs.getInt("coins");

                System.out.println("[COINS] 用户" + userId + " 同步 +" + amount + ", 总计" + total);

                player.sendMessage(MessageProtocol.buildMessage(
                        MessageProtocol.SYNC_OK, String.valueOf(total)));

            } catch (SQLException e) {
                e.printStackTrace();
                player.sendMessage(MessageProtocol.buildMessage(
                        MessageProtocol.SYNC_FAIL, "同步失败"));
            } finally {
                DatabaseManager.closeQuietly(stmt);
            }
        }

        private void handleQueryCoins(String body) {
            int userId;
            try {
                userId = Integer.parseInt(body.trim());
            } catch (NumberFormatException e) {
                return;
            }

            Connection conn = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;

            try {
                conn = db.getConnection();
                stmt = conn.prepareStatement(
                        "SELECT coins, unlocked_pro, unlocked_promax FROM users WHERE id = ?");
                stmt.setInt(1, userId);
                rs = stmt.executeQuery();

                if (rs.next()) {
                    int coins = rs.getInt("coins");
                    int pro = rs.getInt("unlocked_pro");
                    int promax = rs.getInt("unlocked_promax");

                    player.sendMessage(MessageProtocol.buildMessage(
                            MessageProtocol.COINS_INFO,
                            userId + "," + coins + "," + pro + "," + promax));
                } else {
                    player.sendMessage(MessageProtocol.buildMessage(
                            MessageProtocol.COINS_INFO, userId + ",0,0,0"));
                }

            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                DatabaseManager.closeQuietly(rs, stmt);
            }
        }

        private void handleJoinRoom(String body) {
            String playerName;
            int userId = -1;

            if (body.contains(",")) {
                String[] parts = body.split(",", 2);
                try {
                    userId = Integer.parseInt(parts[0].trim());
                    playerName = parts[1].trim();
                } catch (NumberFormatException e) {
                    playerName = body;
                }
            } else {
                playerName = body;
            }

            player.setPlayerName(playerName);

            if (userId > 0) {
                player.setUserId(userId);
                onlineUsers.put(userId, this);
                System.out.println("[JOIN] 用户ID " + userId + " (" + playerName + ") 更新在线状态");
            }

            synchronized (waitingRooms) {
                if (!waitingRooms.isEmpty()) {
                    for (BattleRoom room : waitingRooms) {
                        if (!room.isFull()) {
                            int slot = room.joinPlayer(player);
                            if (slot > 0) {
                                currentRoom = room;
                                if (room.isFull()) {
                                    waitingRooms.remove(room);
                                    activeRooms.add(room);
                                } else {
                                    player.sendMessage(MessageProtocol.buildMessage(
                                            MessageProtocol.WAITING_OPPONENT,
                                            "Please wait for opponent..."));
                                }
                                return;
                            }
                        }
                    }
                }

                BattleRoom newRoom = new BattleRoom();
                newRoom.joinPlayer(player);
                waitingRooms.add(newRoom);
                currentRoom = newRoom;
                player.sendMessage(MessageProtocol.buildMessage(
                        MessageProtocol.WAITING_OPPONENT,
                        "Please wait for opponent..."));
            }
        }

        private void handleScoreUpdate(String scoreStr) {
            int score = Integer.parseInt(scoreStr);
            player.setCurrentScore(score);
            if (currentRoom != null) {
                PlayerSession opponent = currentRoom.getOpponent(player);
                if (opponent != null && !opponent.isClosed()) {
                    opponent.sendMessage(MessageProtocol.buildMessage(
                            MessageProtocol.OPPONENT_SCORE,
                            String.valueOf(score)));
                }
            }
        }

        private void handleGameOver(String finalScoreStr) {
            int finalScore = Integer.parseInt(finalScoreStr);
            player.setFinalScore(finalScore);
            player.setDead();
            if (currentRoom != null) {
                PlayerSession opponent = currentRoom.getOpponent(player);
                if (opponent != null && !opponent.isClosed()) {
                    opponent.sendMessage(MessageProtocol.buildMessage(
                            MessageProtocol.OPPONENT_DEAD,
                            String.valueOf(finalScore)));
                }
                if (currentRoom.bothDead() && !currentRoom.isMatchEnded()) {
                    currentRoom.setMatchEnded(true);
                    finishMatch(currentRoom);
                }
            }
        }

        private void finishMatch(BattleRoom room) {
            PlayerSession p1 = room.getPlayer1();
            PlayerSession p2 = room.getPlayer2();
            if (p1 != null && !p1.isClosed()) {
                int p2Score = (p2 != null) ? p2.getFinalScore() : 0;
                p1.sendMessage(MessageProtocol.buildMessage(
                        MessageProtocol.MATCH_END, String.valueOf(p2Score)));
            }
            if (p2 != null && !p2.isClosed()) {
                int p1Score = (p1 != null) ? p1.getFinalScore() : 0;
                p2.sendMessage(MessageProtocol.buildMessage(
                        MessageProtocol.MATCH_END, String.valueOf(p1Score)));
            }
            activeRooms.remove(room);
        }

        private void handleLeaveRoom() {
            if (currentRoom != null) {
                currentRoom.notifyOpponentLeft(player);
                player.resetForNewGame();
                currentRoom.removePlayer(player);
                if (currentRoom.isEmpty()) {
                    waitingRooms.remove(currentRoom);
                    activeRooms.remove(currentRoom);
                } else {
                    if (!waitingRooms.contains(currentRoom) && !activeRooms.contains(currentRoom)) {
                        waitingRooms.add(currentRoom);
                    }
                }
                currentRoom = null;
                System.out.println("[LEAVE] " + player.getPlayerNameOrId() + " 离开房间，状态已重置");
            }
        }

        private void onDisconnect() {
            handleLeaveRoom();
            if (player.getUserId() > 0) {
                onlineUsers.remove(player.getUserId());
            }
            player.close();
        }
    }
}
