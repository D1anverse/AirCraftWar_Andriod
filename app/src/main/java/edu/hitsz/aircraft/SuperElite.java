package edu.hitsz.aircraft;

import edu.hitsz.bullet.BulletFactory;
import edu.hitsz.strategy.ShootStrategy;


public class SuperElite extends AbstractEnemy{
    public SuperElite(int locationX, int locationY, int speedX, int speedY, int hp, ShootStrategy strategy, BulletFactory bulletFactory) {
        super(locationX, locationY, speedX, speedY, hp, strategy, bulletFactory);
        this.shootInterval = 1500;
        this.power = 30;
        this.direction = 1;
        this.itemAmount = 2;
    }



    @Override
    public int getScores() {
        return 50;
    }

    @Override
    public int bombEffect() {
        this.decreaseHp(25);
        if (this.hp <= 0) {
            return this.getScores();
        }
        return 0;
    }
}
