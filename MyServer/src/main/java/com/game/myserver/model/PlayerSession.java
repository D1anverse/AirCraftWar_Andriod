package com.game.myserver.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class PlayerSession {
    private final String playerId;
    private String playerName;
    private final Socket socket;
    private final BufferedReader reader;
    private final PrintWriter writer;

    // Game state
    private int currentScore = 0;
    private int finalScore = 0;
    private boolean isAlive = true;
    private long lastHeartbeatTime;

    public PlayerSession(String playerId, Socket socket) throws IOException {
        this.playerId = playerId;
        this.socket = socket;
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.writer = new PrintWriter(socket.getOutputStream(), true);
        this.lastHeartbeatTime = System.currentTimeMillis();
    }

    // Getter / Setter

    public String getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String name) { this.playerName = name; }

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
