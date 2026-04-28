package com.game.myserver.model;

import com.game.myserver.protocol.MessageProtocol;
import java.util.UUID;

public class BattleRoom {

    public enum Phase {
        WAITING,
        DIFFICULTY_SELECT,
        AIRCRAFT_SELECT,
        BATTLE
    }

    private final String roomId;
    private PlayerSession player1;
    private PlayerSession player2;
    private boolean isMatchEnded = false;
    private Phase phase = Phase.WAITING;

    private boolean p1DifficultyConfirmed = false;
    private boolean p2DifficultyConfirmed = false;

    private boolean p1AircraftConfirmed = false;
    private boolean p2AircraftConfirmed = false;

    public BattleRoom() {
        this.roomId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public String getRoomId() { return roomId; }
    public boolean isMatchEnded() { return isMatchEnded; }
    public void setMatchEnded(boolean ended) { this.isMatchEnded = ended; }
    public PlayerSession getPlayer1() { return player1; }
    public PlayerSession getPlayer2() { return player2; }
    public Phase getPhase() { return phase; }
    public void setPhase(Phase phase) { this.phase = phase; }

    public synchronized int joinPlayer(PlayerSession player) {
        if (player1 == null) {
            player1 = player;
            System.out.println("  +-- [Room " + roomId + "] " + player.getPlayerNameOrId() + " joined as *Player 1*");
            return 1;

        } else if (player2 == null) {
            player2 = player;
            System.out.println("  +-- [Room " + roomId + "] " + player.getPlayerNameOrId() + " joined as *Player 2*");

            this.phase = Phase.DIFFICULTY_SELECT;
            p1DifficultyConfirmed = false;
            p2DifficultyConfirmed = false;
            p1AircraftConfirmed = false;
            p2AircraftConfirmed = false;

            notifyBothOpponentJoined();
            return 2;
        }
        return -1;
    }

    private void notifyBothOpponentJoined() {
        if (player1 != null && player2 != null) {
            if (!player1.isClosed()) {
                player1.sendMessage(MessageProtocol.buildMessage(
                        MessageProtocol.OPPONENT_JOINED,
                        player2.getPlayerNameOrId()));
            }
            if (!player2.isClosed()) {
                player2.sendMessage(MessageProtocol.buildMessage(
                        MessageProtocol.OPPONENT_JOINED,
                        player1.getPlayerNameOrId()));
            }
            System.out.println("[Room " + roomId + "] Both joined! Entering DIFFICULTY_SELECT phase...");
        }
    }

    public synchronized boolean selectDifficulty(PlayerSession player, String difficulty) {
        if (phase != Phase.DIFFICULTY_SELECT) {
            System.out.println("[Room " + roomId + "] Not in DIFFICULTY_SELECT phase, ignored");
            return false;
        }

        player.setSelectedDifficulty(difficulty);

        boolean isP1 = (player == player1);

        PlayerSession opponent = getOpponent(player);
        if (opponent != null) {
            opponent.sendMessage(MessageProtocol.buildMessage(
                    MessageProtocol.OPPONENT_DIFFICULTY, difficulty));
        }

        if (isP1) {
            p1DifficultyConfirmed = true;
        } else {
            p2DifficultyConfirmed = true;
        }

        if (p1DifficultyConfirmed && p2DifficultyConfirmed) {
            if (player1.getSelectedDifficulty().equals(player2.getSelectedDifficulty())) {
                System.out.println("[Room " + roomId + "] Difficulty matched: " + player1.getSelectedDifficulty());

                player1.sendMessage(MessageProtocol.buildMessage(
                        MessageProtocol.DIFFICULTY_MATCHED, ""));
                player2.sendMessage(MessageProtocol.buildMessage(
                        MessageProtocol.DIFFICULTY_MATCHED, ""));

                this.phase = Phase.AIRCRAFT_SELECT;
                p1AircraftConfirmed = false;
                p2AircraftConfirmed = false;
                player1.setAircraftConfirmed(false);
                player2.setAircraftConfirmed(false);

                System.out.println("[Room " + roomId + "] Entering AIRCRAFT_SELECT phase...");
                return true;
            } else {
                System.out.println("[Room " + roomId + "] Difficulty mismatch! P1=" + player1.getSelectedDifficulty()
                        + ", P2=" + player2.getSelectedDifficulty());

                player1.sendMessage(MessageProtocol.buildMessage(
                        MessageProtocol.DIFFICULTY_RETRY, ""));
                player2.sendMessage(MessageProtocol.buildMessage(
                        MessageProtocol.DIFFICULTY_RETRY, ""));

                p1DifficultyConfirmed = false;
                p2DifficultyConfirmed = false;
                player1.setDifficultyConfirmed(false);
                player2.setDifficultyConfirmed(false);
                player1.setSelectedDifficulty(null);
                player2.setSelectedDifficulty(null);
                return false;
            }
        }
        return false;
    }

    public synchronized boolean selectAircraft(PlayerSession player, String aircraftType) {
        if (phase != Phase.AIRCRAFT_SELECT) {
            System.out.println("[Room " + roomId + "] Not in AIRCRAFT_SELECT phase, ignored");
            return false;
        }

        player.setSelectedAircraft(aircraftType);

        boolean isP1 = (player == player1);

        PlayerSession opponent = getOpponent(player);
        if (opponent != null) {
            opponent.sendMessage(MessageProtocol.buildMessage(
                    MessageProtocol.OPPONENT_AIRCRAFT_SELECTED, aircraftType));
        }

        if (isP1) {
            p1AircraftConfirmed = true;
        } else {
            p2AircraftConfirmed = true;
        }

        if (p1AircraftConfirmed && p2AircraftConfirmed) {
            System.out.println("[Room " + roomId + "] Both confirmed aircraft! Starting battle...");

            player1.sendMessage(MessageProtocol.buildMessage(
                    MessageProtocol.READY_TO_FIGHT, ""));
            player2.sendMessage(MessageProtocol.buildMessage(
                    MessageProtocol.READY_TO_FIGHT, ""));

            this.phase = Phase.BATTLE;
            return true;
        }
        return false;
    }

    public synchronized void removePlayer(PlayerSession player) {
        if (player == player1) {
            player1 = null;
        } else if (player == player2) {
            player2 = null;
        }
    }

    public boolean isFull() { return player1 != null && player2 != null; }

    public boolean isEmpty() { return player1 == null && player2 == null; }

    public PlayerSession getOpponent(PlayerSession me) {
        if (me == player1) return player2;
        if (me == player2) return player1;
        return null;
    }

    public boolean bothDead() {
        return player1 != null && player2 != null
                && !player1.isAlive() && !player2.isAlive();
    }

    public void notifyOpponentLeft(PlayerSession leaver) {
        PlayerSession opponent = getOpponent(leaver);
        if (opponent != null && !opponent.isClosed()) {
            opponent.sendMessage(MessageProtocol.buildMessage(
                    MessageProtocol.OPPONENT_LEFT, leaver.getPlayerNameOrId()));
        }
    }

    public void notifyRoomCancelled() {
        if (player1 != null && !player1.isClosed()) {
            player1.sendMessage(MessageProtocol.buildMessage(
                    MessageProtocol.ROOM_CANCELLED, ""));
        }
        if (player2 != null && !player2.isClosed()) {
            player2.sendMessage(MessageProtocol.buildMessage(
                    MessageProtocol.ROOM_CANCELLED, ""));
        }
    }

    public void reset() {
        this.phase = Phase.WAITING;
        p1DifficultyConfirmed = false;
        p2DifficultyConfirmed = false;
        p1AircraftConfirmed = false;
        p2AircraftConfirmed = false;
        isMatchEnded = false;
        if (player1 != null) {
            player1.resetForNewGame();
        }
        if (player2 != null) {
            player2.resetForNewGame();
        }
    }
}
