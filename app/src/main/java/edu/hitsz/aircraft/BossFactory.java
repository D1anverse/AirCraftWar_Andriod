package edu.hitsz.aircraft;

import edu.hitsz.application.GameConfig;
import edu.hitsz.application.ImageManager;
import edu.hitsz.bullet.EnemyBulletFactory;
import edu.hitsz.strategy.ShootCircle;

public class BossFactory implements EnemyFactory{

    @Override
    public AbstractEnemy createEnemy(int bossHp) {
        int screenWidth = GameConfig.getInstance().getScreenWidth();
        int screenHeight = GameConfig.getInstance().getScreenHeight();
        return new BossEnemy(
                (int)(ImageManager.BOSS_IMAGE.getWidth()*0.5+Math.random() * (screenWidth - ImageManager.BOSS_IMAGE.getWidth())),
                (int)(Math.random()* screenHeight * 0.2),
                3,
                0,
                bossHp,
                new ShootCircle(),
                new EnemyBulletFactory()
        );
    }
}
