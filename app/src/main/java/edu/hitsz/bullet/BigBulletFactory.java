package edu.hitsz.bullet;

public class BigBulletFactory implements BulletFactory{
    @Override
    public BaseBullet createBullet(int x, int y, int speedX, int speedY, int power) {
        return new BigHeroBullet(x, y, speedX, speedY, power);
    }
}
