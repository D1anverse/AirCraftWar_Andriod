package edu.hitsz.bullet;

public class HeroBulletFactory implements BulletFactory{
    @Override
    public BaseBullet createBullet(int x, int y, int speedX, int speedY, int power) {
        return new HeroBullet(x, y, speedX, speedY, power);
    }
}
