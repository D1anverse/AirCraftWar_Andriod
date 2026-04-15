package edu.hitsz.application;

import android.content.Context;
import edu.hitsz.R;

public class GameNormal extends Game {

    public GameNormal(Context context, String musicMode, String userName, GameOverListener listener) {
        super(context, musicMode, userName, listener);
        this.gameLevel = 1;
        setBackground(ImageManager.BACKGROUND_IMAGE_NORMAL);
    }

    @Override
    protected void initHeroAircraft() {
        heroAircraft.init(screenWidth / 2,
                screenHeight - ImageManager.HERO_IMAGE.getHeight(),
                0, 0, 300); // 困难难度初始 80 生命值
    }

    // 使用父类的默认 checkForBossSpawn 和 increaseDifficulty 实现
}