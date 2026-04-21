package com.game.myserver;

import com.game.myserver.model.BattleRoom;
import com.game.myserver.model.PlayerSession;
import com.game.myserver.protocol.MessageProtocol;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AirWar Battle Server v1.0
 *
 * Usage:
 * 1. Run main() to start the server
 * 2. Default port: 8080
 * 3. Mobile client connects via PC IP + 8080
 *
 * Note: Ensure mobile and PC are on the same LAN/WiFi!
 */

public class MyClass {

    /** 服务器监听端口号 */
    private static final int PORT = 8080;

    private ServerSocket serverSocket;
    private ExecutorService threadPool;       // 线程池（每个客户端分配一个线程）
    private volatile boolean running = true;   // 服务器运行标志

    /** 等待中的房间列表（只有1人的房间，等待第2人来） */
    private final List<BattleRoom> waitingRooms = new CopyOnWriteArrayList<>();

    /** 正在对战中的活跃房间 */
    private final List<BattleRoom> activeRooms = new CopyOnWriteArrayList<>();

    /** 玩家ID自增器 (Player_1, Player_2, ...) */
    private final AtomicInteger playerIdCounter = new AtomicInteger(0);

    // 启动入口

    public static void main(String[] args) {
        new MyClass().start();
    }

    /**
     * 启动服务器 - 开始监听端口，接受客户端连接
     */
    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            threadPool = Executors.newCachedThreadPool();

            printStartupInfo();

            // 主循环: 持续接受新连接
            while (running) {
                Socket clientSocket = serverSocket.accept();

                String playerId = "Player_" + playerIdCounter.incrementAndGet();
                System.out.println("\n新连接 → " + playerId);

                threadPool.execute(new ClientHandler(clientSocket, playerId));
            }

        } catch (IOException e) {
            if (running) {
                System.err.println("\nServer error: " + e.getMessage());
                e.printStackTrace();
            }
        } finally {
            stop();
        }
    }

    /**
     * 停止服务器
     */
    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (threadPool != null) threadPool.shutdownNow();
        System.out.println("\nServer stopped");
    }

    /**
     * Print startup info
     */
    private void printStartupInfo() {
        System.out.println("");
        System.out.println("========================================");
        System.out.println("||                                      ||");
        System.out.println("||       AirWar Battle Server v1.0      ||");
        System.out.println("||                                      ||");
        System.out.println("========================================");
        System.out.println("");
        System.out.println("[OK] Server started on port: " + PORT);
        System.out.println("[..] Waiting for players...");
        System.out.println("----------------------------------------");
    }



    // 客户端处理器

    /**
     * 每一个连接进来的客户端对应一个 Handler 实例
     * 运行在独立线程中，负责持续读取该客户端的消息并处理
     */
    private class ClientHandler implements Runnable {

        private final Socket socket;
        private final PlayerSession player;
        private BattleRoom currentRoom;   // 该玩家当前所在的房间

        public ClientHandler(Socket socket, String playerId) throws IOException {
            this.socket = socket;
            this.player = new PlayerSession(playerId, socket);
        }

        @Override
        public void run() {
            try {
                String message;

                // 循环读取客户端发来的每一行消息
                while ((message = player.receiveMessage()) != null) {
                    processMessage(message);
                }

            } catch (IOException e) {
                System.out.println("断开 → " + player.getPlayerNameOrId() + " (" + e.getMessage() + ")");
            } finally {
                onDisconnect();
            }
        }

        /**
         * 处理收到的单条消息（根据消息类型分发）
         */
        private void processMessage(String rawMessage) {
            System.out.println("[DEBUG] Raw message: " + rawMessage);

            String[] parts = MessageProtocol.parseMessage(rawMessage);
            if (parts == null || parts.length < 2) {
                System.out.println("[DEBUG] Parse failed! raw=" + rawMessage);
                return;
            }

            String type = parts[0];
            String body = parts[1];

            System.out.println("[DEBUG] Parsed -> type=" + type + ", body=" + body);

            switch (type) {
                case MessageProtocol.JOIN_ROOM:
                    System.out.println("[DEBUG] Handling join room request!");
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

                default:
                    System.out.println("Unknown message type: " + type);
            }
        }

        // 处理各消息类型

        /**
         * 处理: 玩家请求加入房间
         * 逻辑: 先找有没有等人的房间，没有就新建一个
         */
        private void handleJoinRoom(String playerName) {
            player.setPlayerName(playerName);

            synchronized (waitingRooms) {
                // Step 1: Try to join an existing waiting room
                if (!waitingRooms.isEmpty()) {
                    for (BattleRoom room : waitingRooms) {
                        if (!room.isFull()) {
                            int slot = room.joinPlayer(player);
                            if (slot > 0) {
                                currentRoom = room;

                                System.out.println("[MATCH] " + playerName
                                        + " joined room " + room.getRoomId()
                                        + " as Player " + slot);

                                if (room.isFull()) {
                                    waitingRooms.remove(room);
                                    activeRooms.add(room);

                                    System.out.println("[ROOM] " + room.getRoomId()
                                            + " is full, battle starting!");
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

                // Step 2: No available room, create new one
                BattleRoom newRoom = new BattleRoom();
                newRoom.joinPlayer(player);
                waitingRooms.add(newRoom);
                currentRoom = newRoom;

                System.out.println("[NEW] " + playerName
                        + " created room " + newRoom.getRoomId()
                        + ", waiting for opponent...");

                player.sendMessage(MessageProtocol.buildMessage(
                        MessageProtocol.WAITING_OPPONENT,
                        "Please wait for opponent..."));
            }
        }

        /**
         * Handle score update, forward to opponent
         */
        private void handleScoreUpdate(String scoreStr) {
            int score = Integer.parseInt(scoreStr);
            player.setCurrentScore(score);

            if (currentRoom != null) {
                PlayerSession opponent = currentRoom.getOpponent(player);
                if (opponent != null && !opponent.isClosed()) {
                    String forwardMsg = MessageProtocol.buildMessage(
                            MessageProtocol.OPPONENT_SCORE,
                            String.valueOf(score));
                    opponent.sendMessage(forwardMsg);
                }
            }
        }

        /**
         * Handle player death, notify opponent, check if match ended
         */
        private void handleGameOver(String finalScoreStr) {
            int finalScore = Integer.parseInt(finalScoreStr);
            player.setFinalScore(finalScore);
            player.setDead();

            System.out.println(player.getPlayerNameOrId()
                    + " eliminated! Score: " + finalScore);

            if (currentRoom != null) {
                PlayerSession opponent = currentRoom.getOpponent(player);

                if (opponent != null && !opponent.isClosed()) {
                    String deathMsg = MessageProtocol.buildMessage(
                            MessageProtocol.OPPONENT_DEAD,
                            String.valueOf(finalScore));
                    opponent.sendMessage(deathMsg);
                }

                if (currentRoom.bothDead() && !currentRoom.isMatchEnded()) {
                    currentRoom.setMatchEnded(true);
                    finishMatch(currentRoom);
                }
            }
        }

        /**
         * Match ended, send final results to both players
         */
        private void finishMatch(BattleRoom room) {
            System.out.println("\n=== Match ended! Room: " + room.getRoomId() + " ===");

            PlayerSession p1 = room.getPlayer1();
            PlayerSession p2 = room.getPlayer2();

            if (p1 != null && !p1.isClosed()) {
                int p2Score = (p2 != null) ? p2.getFinalScore() : 0;
                p1.sendMessage(MessageProtocol.buildMessage(
                        MessageProtocol.MATCH_END,
                        String.valueOf(p2Score)));
                System.out.println("-> " + p1.getPlayerNameOrId()
                        + " Score: " + p1.getFinalScore());
            }

            if (p2 != null && !p2.isClosed()) {
                int p1Score = (p1 != null) ? p1.getFinalScore() : 0;
                p2.sendMessage(MessageProtocol.buildMessage(
                        MessageProtocol.MATCH_END,
                        String.valueOf(p1Score)));
                System.out.println("-> " + p2.getPlayerNameOrId()
                        + " Score: " + p2.getFinalScore());
            }

            int winner = room.getPlayer1().getFinalScore() > room.getPlayer2().getFinalScore() ? 1 : 2;
            System.out.println("Winner: Player " + winner);
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

            activeRooms.remove(room);
        }

        /**
         * Handle player leaving room
         */
        private void handleLeaveRoom() {
            if (currentRoom != null) {
                currentRoom.removePlayer(player);
                if (currentRoom.isEmpty()) {
                    waitingRooms.remove(currentRoom);
                    activeRooms.remove(currentRoom);
                    System.out.println("Room " + currentRoom.getRoomId() + " destroyed");
                }
                currentRoom = null;
            }
        }

        /**
         * 连接断开时的清理工作
         */
        private void onDisconnect() {
            handleLeaveRoom();
            player.close();
        }
    }
}
