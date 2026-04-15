package edu.hitsz.strategy;

import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.bullet.BulletFactory;

import java.util.LinkedList;
import java.util.List;

public class ShootMultiStraight implements ShootStrategy {
    @Override
    public List<BaseBullet> doShoot(int x, int y, int direction, int power, BulletFactory factory) {
        int shootNum = 3;
        List<BaseBullet> res = new LinkedList<>();
        y = y + direction*2;
        int speedX = 0;
        int speedY = direction*8;
        BaseBullet bullet;
        for(int i=-1; i<shootNum - 1; i++){
            // 子弹发射位置相对飞机位置向前偏移
            // 多个子弹横向分散
            bullet = factory.createBullet(x + i * 20, y, speedX+i, speedY, power);
            res.add(bullet);
        }
        return res;
    }
}
