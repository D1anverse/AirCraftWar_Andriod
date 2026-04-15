package edu.hitsz.aircraft;

import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.bullet.BulletFactory;
import edu.hitsz.basic.AbstractFlyingObject;
import edu.hitsz.strategy.ShootStrategy;

import java.util.LinkedList;
import java.util.List;

/**
 * 所有种类飞机的抽象父类：
 * 敌机（BOSS, ELITE, MOB），英雄飞机
 *
 * @author hitsz
 */
public abstract class AbstractAircraft extends AbstractFlyingObject {
    /**
     * 生命值
     */
    protected ShootStrategy strategy;
    protected BulletFactory bulletFactory;
    protected int maxHp;
    protected int hp;
    protected long lastShootTime = 0; // 记录上一次射击的时间
    protected long shootInterval = 2000;

    //子弹伤害
    protected int power;
    //子弹方向s
    protected int direction;

    public AbstractAircraft(int locationX, int locationY, int speedX, int speedY, int hp, ShootStrategy strategy, BulletFactory bulletFactory) {
        super(locationX, locationY, speedX, speedY);
        this.hp = hp;
        this.maxHp = hp;
        this.strategy = strategy;
        this.bulletFactory = bulletFactory;
    }

    public void decreaseHp(int decrease){
        if (decrease>0) {
            hp -= decrease;
            if (hp <= 0) {
                hp = 0;
                vanish();
            }
        }
    }

    public int getHp() {
        return hp;
    }




    public void setStrategy(ShootStrategy strategy) {
        this.strategy = strategy;
    }

    public List<BaseBullet> executeStrategy() {
        // 获取当前时间
        long currentTime = System.currentTimeMillis();

        // 检查是否达到射击间隔
        if (currentTime - lastShootTime < shootInterval) {
            return new LinkedList<>(); // 返回空列表，不射击
        }
        // 更新上一次射击时间
        lastShootTime = currentTime;

        return strategy.doShoot(this.getLocationX(), this.getLocationY(),this.direction, this.power, this.bulletFactory);
    }
}


