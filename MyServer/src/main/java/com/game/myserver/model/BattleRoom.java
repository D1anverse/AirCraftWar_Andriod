package com.game.myserver.model;

import com.game.myserver.protocol.MessageProtocol;

import java.util.UUID;

/**
 * Battle room - manages two players' matching and game state
 *
 * State flow:
 * Created -> [P1 joins] -> Waiting -> [P2 joins] -> Ready -> Battle -> [Both dead] -> End
 */
public class BattleRoom {

    private final String roomId;
    private PlayerSession player1;
    private PlayerSession player2;
    private boolean isMatchEnded = false;

    public BattleRoom() {
        // Generate 8-char room ID with uppercase letters and numbers, e.g., A3K7M2X9
        this.roomId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public String getRoomId() { return roomId; }
    public boolean isMatchEnded() { return isMatchEnded; }
    public void setMatchEnded(boolean ended) { this.isMatchEnded = ended; }
    public PlayerSession getPlayer1() { return player1; }
    public PlayerSession getPlayer2() { return player2; }

    /**
     * Player joins room
     * @param player The player joining
     * @return 1=as player 1, 2=as player 2, -1=room full
     */
    public synchronized int joinPlayer(PlayerSession player) {
        if (player1 == null) {
            player1 = player;
            System.out.println("  +-- [Room " + roomId + "] " + player.getPlayerNameOrId() + " joined as *Player 1*");
            return 1;

        } else if (player2 == null) {
            player2 = player;
            System.out.println("  +-- [Room " + roomId + "] " + player.getPlayerNameOrId() + " joined as *Player 2*");
            notifyBothOpponentJoined();
            return 2;
        }

        return -1;
    }

    /**
     * Remove specified player
     */
    public synchronized void removePlayer(PlayerSession player) {
        if (player == player1) {
            player1 = null;
        } else if (player == player2) {
            player2 = null;
        }
    }

    /** Check if room is full (2 players) */
    public boolean isFull() { return player1 != null && player2 != null; }

    /** Check if room is empty */
    public boolean isEmpty() { return player1 == null && player2 == null; }

    /**
     * Get opponent
     * @param me Myself
     * @return Opponent's PlayerSession, null if not found
     */
    public PlayerSession getOpponent(PlayerSession me) {
        if (me == player1) return player2;
        if (me == player2) return player1;
        return null;
    }

    /** Check if both players are dead */
    public boolean bothDead() {
        return player1 != null && player2 != null
                && !player1.isAlive() && !player2.isAlive();
    }

    /**
     * When both players are ready, notify each other that opponent has joined
     */
    private void notifyBothOpponentJoined() {
        if (player1 != null && player2 != null) {
            // Tell player 1: your opponent is player 2
            String msgToP1 = MessageProtocol.buildMessage(
                    MessageProtocol.OPPONENT_JOINED,
                    player2.getPlayerNameOrId());
            player1.sendMessage(msgToP1);

            // Tell player 2: your opponent is player 1
            String msgToP2 = MessageProtocol.buildMessage(
                    MessageProtocol.OPPONENT_JOINED,
                    player1.getPlayerNameOrId());
            player2.sendMessage(msgToP2);

            System.out.println("[Room " + roomId + "] Both ready! Battle starting!");
        }
    }
}
