package edu.hitsz.strategy;

import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.bullet.BulletFactory;

import java.util.List;

public interface ShootStrategy {
    List<BaseBullet> doShoot(int x, int y, int direction, int power, BulletFactory factory);
}
