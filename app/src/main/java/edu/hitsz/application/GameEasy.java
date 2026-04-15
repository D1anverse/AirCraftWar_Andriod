package edu.hitsz.application;

import android.content.Context;
import edu.hitsz.R;

public class GameEasy extends Game {

    public GameEasy(Context context, String musicMode, String userName, GameOverListener listener) {
        super(context, musicMode, userName, listener);
        this.gameLevel = 0;
        setBackground(ImageManager.BACKGROUND_IMAGE_EASY);
    }

    @Override
    protected void checkForBossSpawn() {
        // 简单模式不生成 Boss，因此留空
    }

    @Override
    protected void increaseDifficulty() {
        // 简单模式不随时间增加难度，因此留空
    }

    @Override
    protected void initHeroAircraft() {
        heroAircraft.init(screenWidth / 2,
                screenHeight - ImageManager.HERO_IMAGE.getHeight(),
                0, 0, 500); // 困难难度初始 500 生命值
    }
}