package edu.hitsz.dao;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Score {
    private int id;
    private final String username;
    private final int score;
    private String time;

    public Score(int id, String username, int score, String time) {
        this.id = id;
        this.username = username;
        this.score = score;
        this.time = time;
    }

    public Score(String username, int score) {
        this.username = username;
        this.score = score;
        // 自动生成时间
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        this.time = sdf.format(new Date());
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public int getScore() {
        return score;
    }


    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public void info() {
        System.out.println("ID[" + id + "], UserName:[" + username + "], Score[" + score + "], time[" + time + "]"+"\n");
    }
}