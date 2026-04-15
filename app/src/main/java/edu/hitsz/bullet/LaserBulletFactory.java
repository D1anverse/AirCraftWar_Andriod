package edu.hitsz.bullet;

public class LaserBulletFactory implements BulletFactory{
    @Override
    public BaseBullet createBullet(int x, int y, int speedX, int speedY, int power) {
        return new LaserBullet(x, y, power);
    }
}
