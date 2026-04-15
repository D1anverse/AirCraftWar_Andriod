package edu.hitsz.bullet;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import edu.hitsz.application.GameConfig;
import edu.hitsz.application.ImageManager;

/**
 * 激光子弹 - ProMax版火力道具触发
 * 穿透敌人，持续伤害
 */
public class LaserBullet extends BaseBullet {
    private int laserWidth;
    private int laserHeight;
    private int damageInterval; // 伤害间隔
    private long lastDamageTime;

    public LaserBullet(int locationX, int locationY, int power) {
        super(locationX, locationY, 0, 0, power);
        this.laserWidth = 20;
        this.laserHeight = GameConfig.getInstance().getScreenHeight();
        this.damageInterval = 100; // 每100ms造成一次伤害
        this.lastDamageTime = 0;
    }

    @Override
    public void forward() {
        // 激光不移动，跟随英雄机
        // 位置在Game中更新
    }

    /**
     * 激光是否可以造成伤害
     */
    public boolean canDamage() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastDamageTime >= damageInterval) {
            lastDamageTime = currentTime;
            return true;
        }
        return false;
    }

    /**
     * 激光不会因为碰撞而消失
     */
    @Override
    public void vanish() {
        // 激光不会vanish，只能通过技能结束消失
    }

    @Override
    public int getWidth() {
        return laserWidth;
    }

    @Override
    public int getHeight() {
        return laserHeight;
    }

    /**
     * 激光是穿透的，不vanish
     */
    public void realVanish() {
        super.vanish();
    }

    @Override
    public int bombEffect() {
        return 0;
    }

    @Override
    public Bitmap getImage() {
        return ImageManager.LASER_IMAGE;
    }
}

