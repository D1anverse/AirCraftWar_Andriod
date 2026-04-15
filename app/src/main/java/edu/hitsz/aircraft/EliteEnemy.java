package edu.hitsz.aircraft;

import edu.hitsz.bullet.BulletFactory;
import edu.hitsz.strategy.ShootStrategy;



//精英敌机
public class EliteEnemy extends AbstractEnemy{



    public EliteEnemy(int locationX, int locationY, int speedX, int speedY, int hp, ShootStrategy strategy, BulletFactory bulletFactory) {
        super(locationX, locationY, speedX, speedY, hp, strategy, bulletFactory);
        this.shootInterval = 800;
        this.power = 30;
        this.direction = 1;
        this.itemAmount = 2;
    }


    @Override
    public int getScores(){
        return 30;
    }

    @Override
    public int bombEffect() {
        this.vanish();
        return this.getScores();
    }

}
