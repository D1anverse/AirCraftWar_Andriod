package edu.hitsz.bullet;

public class EnemyBulletFactory implements BulletFactory{
    @Override
    public BaseBullet createBullet(int x, int y, int speedX, int speedY, int power) {
        return new EnemyBullet(x, y, speedX, speedY, power);
    }
}
