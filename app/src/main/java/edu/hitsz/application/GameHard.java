package edu.hitsz.application;

import android.content.Context;
import edu.hitsz.R;

import edu.hitsz.aircraft.AbstractEnemy;
import edu.hitsz.aircraft.BossFactory;
import edu.hitsz.aircraft.EnemyFactory;

public class GameHard extends Game {

    private int bossCounter = 0; // 记录Boss生成次数，用于递增血量

    public GameHard(Context context, String musicMode, String userName, GameOverListener listener) {
        super(context, musicMode, userName, listener);
        this.gameLevel = 2;
        setBackground(ImageManager.BACKGROUND_IMAGE_HARD);
    }

    @Override
    protected void checkForBossSpawn() {
        int deltaScore = score - lastBossScore;
        if (deltaScore > bossInterval && !isBossExist) {
            // 每次生成Boss后缩短生成间隔（不低于300）
            if (bossInterval > 300) {
                bossInterval -= 30;
                System.out.println("Boss生成间隔降低: " + bossInterval);
            }
            // Boss血量随出现次数递增
            int bossHp = 600 + bossCounter * 100;
            lastBossScore = score;
            AbstractEnemy enemy;
            EnemyFactory factory = new BossFactory();
            enemy = factory.createEnemy(bossHp);
            enemyAircrafts.add(enemy);
            isBossExist = true;
            playSoundManager.stopBgm();
            playSoundManager.playBgmBoss();
            bossCounter++;
            System.out.println("Boss血量提升: " + bossHp);
        } else if (isBossExist) {
            lastBossScore = score;
        }
    }

    @Override
    protected void initHeroAircraft() {
        heroAircraft.init(screenWidth / 2,
                screenHeight - ImageManager.HERO_IMAGE.getHeight(),
                0, 0, 200); // 困难难度初始 80 生命值
    }

    // increaseDifficulty 直接使用父类的默认实现（已包含 gameLevel 的影响）
}