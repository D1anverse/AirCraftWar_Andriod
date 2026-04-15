package edu.hitsz.strategy;

import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.bullet.BulletFactory;
import edu.hitsz.bullet.LaserBullet;
import edu.hitsz.bullet.LaserBulletFactory;
import java.util.LinkedList;
import java.util.List;

/**
 * 激光射击策略 - ProMax版火力道具触发
 * 从两侧发射激光束
 */
public class ShootLaser implements ShootStrategy {
    private int wingOffset;

    public ShootLaser() {
        this.wingOffset = 40;
    }

    @Override
    public List<BaseBullet> doShoot(int x, int y, int direction, int power, BulletFactory factory) {
        List<BaseBullet> res = new LinkedList<>();

        // 直接创建激光子弹，不依赖工厂类型
        // 左翼激光
        res.add(new LaserBullet(x - wingOffset, y, power));
        // 右翼激光
        res.add(new LaserBullet(x + wingOffset, y, power));

        return res;
    }
}
