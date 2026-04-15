package edu.hitsz.strategy;

import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.bullet.BulletFactory;

import java.util.LinkedList;
import java.util.List;

public class ShootNull implements ShootStrategy {
    @Override
    public List<BaseBullet> doShoot(int x, int y, int direction, int power, BulletFactory factory) {
        return new LinkedList<>();
    }
}
