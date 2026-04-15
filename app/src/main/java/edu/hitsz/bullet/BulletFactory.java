package edu.hitsz.bullet;

public interface BulletFactory {

    BaseBullet createBullet(int x, int y, int speedX, int speedY, int power);

}
