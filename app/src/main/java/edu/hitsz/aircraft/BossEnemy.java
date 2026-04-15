package edu.hitsz.aircraft;

import edu.hitsz.application.GameConfig;
import edu.hitsz.application.ImageManager;
import edu.hitsz.bullet.BulletFactory;
import edu.hitsz.strategy.ShootStrategy;

public class BossEnemy extends AbstractEnemy{

    int screenWidth = GameConfig.getInstance().getScreenWidth();
    private final int minX = (int)(ImageManager.BOSS_IMAGE.getWidth()*0.5); // 最小X边界
    private final int maxX = (int)(screenWidth - ImageManager.BOSS_IMAGE.getWidth()*0.5);

    public BossEnemy(int locationX, int locationY, int speedX, int speedY, int hp, ShootStrategy strategy, BulletFactory bulletFactory) {
        super(locationX, locationY, speedX, speedY, hp, strategy, bulletFactory);
        this.shootInterval = 3000;
        this.power = 30;
        this.direction = 1;
        this.itemAmount = 4;
    }




    @Override
    public void forward() {
        locationX += speedX;
        locationY += speedY;
        if (locationX <= minX || locationX >= maxX) {
            // 横向超出边界后反向
            speedX = -speedX;
        }
    }

    @Override
    public int getScores() {
        return 150;
    }

    @Override
    public int bombEffect() {
        return 0;
    }
}

