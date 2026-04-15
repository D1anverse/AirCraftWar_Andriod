package edu.hitsz.aircraft;

import edu.hitsz.application.GameConfig;
import edu.hitsz.application.ImageManager;
import edu.hitsz.bullet.EnemyBulletFactory;
import edu.hitsz.strategy.ShootMultiStraight;

public class SuperEliteFactory implements EnemyFactory{
    @Override
    public AbstractEnemy createEnemy(int hp) {
        int screenWidth = GameConfig.getInstance().getScreenWidth();
        int screenHeight = GameConfig.getInstance().getScreenHeight();
        return new SuperElite(
                (int)(ImageManager.SUPER_ELITE_IMAGE.getWidth()*0.5+Math.random() * (screenWidth - ImageManager.SUPER_ELITE_IMAGE.getWidth())),
                (int)(Math.random()*screenHeight * 0.05),
                0,
                3,
                hp,
                new ShootMultiStraight(),
                new EnemyBulletFactory()

        );
    }
}
