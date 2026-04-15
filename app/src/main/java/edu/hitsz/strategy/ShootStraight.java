package edu.hitsz.strategy;

import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.bullet.BulletFactory;

import java.util.LinkedList;
import java.util.List;

public class ShootStraight implements ShootStrategy {
    @Override
    public List<BaseBullet> doShoot(int x, int y, int direction, int power, BulletFactory factory) {
        List<BaseBullet> res = new LinkedList<>();
        y = y + direction;
        int speedX = 0;
        int speedY = direction*5;
        BaseBullet bullet;
        bullet = factory.createBullet(x, y, speedX, speedY, power);
        res.add(bullet);

        return res;
    }
}
