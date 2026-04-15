package edu.hitsz.strategy;

import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.bullet.BulletFactory;
import java.util.LinkedList;
import java.util.List;

/**
 * 双翼射击策略 - ProMax版专属
 * 从飞机两侧机翼发射子弹
 */
public class ShootDoubleWing implements ShootStrategy {
    private int wingOffset; // 机翼偏移量

    public ShootDoubleWing() {
        this.wingOffset = 40; // 默认偏移
    }

    public ShootDoubleWing(int wingOffset) {
        this.wingOffset = wingOffset;
    }

    @Override
    public List<BaseBullet> doShoot(int x, int y, int direction, int power, BulletFactory factory) {
        List<BaseBullet> res = new LinkedList<>();
        int speedY = direction * 8;

        // 左翼发射
        BaseBullet leftBullet = factory.createBullet(
                x - wingOffset, y, 0, speedY, power
        );
        res.add(leftBullet);

        // 右翼发射
        BaseBullet rightBullet = factory.createBullet(
                x + wingOffset, y, 0, speedY, power
        );
        res.add(rightBullet);

        return res;
    }
}

