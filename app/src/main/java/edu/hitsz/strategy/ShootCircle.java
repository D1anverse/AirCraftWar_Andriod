package edu.hitsz.strategy;

import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.bullet.BulletFactory;

import java.util.LinkedList;
import java.util.List;

public class ShootCircle implements ShootStrategy {
    @Override
    public List<BaseBullet> doShoot(int x, int y, int direction, int power, BulletFactory factory) {
        int shootNum = 20;
        List<BaseBullet> enemyAttack = new LinkedList<>();
        y = y + direction*2;
        BaseBullet bullet;
        for(int i = 0; i < shootNum; i++) {
            // 计算每个子弹的角度（均匀分布在一圈）
            double angle = 2 * Math.PI * i / shootNum;
            double radius = 100;
            double bulletX = x + radius * Math.cos(angle);
            double bulletY = y + radius * Math.sin(angle);
            double speed = 5.0; // 速度大小

            int speedX = (int)(speed * Math.cos(angle));
            int speedY = (int)(speed * Math.sin(angle));
            bullet = factory.createBullet((int)bulletX, (int)bulletY, speedX, speedY, power);
            enemyAttack.add(bullet);
        }
        return enemyAttack;
    }
}
