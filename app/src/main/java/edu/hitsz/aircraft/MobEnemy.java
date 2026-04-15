package edu.hitsz.aircraft;

import edu.hitsz.bullet.BulletFactory;
import edu.hitsz.item.BaseItem;
import edu.hitsz.strategy.ShootStrategy;

import java.util.LinkedList;
import java.util.List;

/**
 * 普通敌机
 * 不可射击
 *
 * @author hitsz
 */
public class MobEnemy extends AbstractEnemy {

    public MobEnemy(int locationX, int locationY, int speedX, int speedY, int hp, ShootStrategy strategy, BulletFactory bulletFactory) {
        super(locationX, locationY, speedX, speedY, hp, strategy, bulletFactory);
        this.itemAmount = 1;
    }


    @Override
    public List<BaseItem> spawnItems() {return new LinkedList<>();}

    @Override
    public int getScores(){
        return 10;
    }

    @Override
    public int bombEffect() {
        this.vanish();
        return this.getScores();
    }
}
