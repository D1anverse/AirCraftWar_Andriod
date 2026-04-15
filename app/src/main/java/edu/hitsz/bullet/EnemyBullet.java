package edu.hitsz.bullet;


public class EnemyBullet extends BaseBullet {

    public EnemyBullet(int locationX, int locationY, int speedX, int speedY, int power) {
        super(locationX, locationY, speedX, speedY, power);
    }

    @Override
    public int bombEffect() {
        this.vanish();
        return 0;
    }
}
