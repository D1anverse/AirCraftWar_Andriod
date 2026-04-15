package edu.hitsz.aircraft;

import edu.hitsz.application.GameConfig;
import edu.hitsz.application.ImageManager;
import edu.hitsz.bullet.EnemyBulletFactory;
import edu.hitsz.strategy.ShootStraight;


public class EliteEnemyFactory implements EnemyFactory {
    @Override
    public AbstractEnemy createEnemy(int hp) {
        int screenWidth = GameConfig.getInstance().getScreenWidth();
        int screenHeight = GameConfig.getInstance().getScreenHeight();
        return new EliteEnemy(
                (int)(ImageManager.Elite_ENEMY_IMAGE.getWidth()*0.5+Math.random() * (screenWidth - ImageManager.Elite_ENEMY_IMAGE.getWidth())),
                (int)(Math.random()*screenHeight * 0.05),
                0,
                3,
                hp,
                new ShootStraight(),
                new EnemyBulletFactory()
        );
    }
}
