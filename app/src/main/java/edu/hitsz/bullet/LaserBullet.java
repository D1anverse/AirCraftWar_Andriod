package edu.hitsz.bullet;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import edu.hitsz.application.GameConfig;
import edu.hitsz.application.ImageManager;

/**
 * 激光子弹 - ProMax版火力道具触发
 * 穿透敌人，每帧持续伤害
 */
public class LaserBullet extends BaseBullet {
    private int laserWidth;
    private int laserHeight;
    private int frameDamageCount; // 每帧累积伤害计数
    private int damagePerFrame = 5; // 每帧累积伤害

    public LaserBullet(int locationX, int locationY, int power) {
        super(locationX, locationY, 0, 0, power);
        this.laserWidth = 20;
        this.laserHeight = GameConfig.getInstance().getScreenHeight();
        this.frameDamageCount = 0;
    }

    @Override
    public void forward() {
        // 激光不移动，跟随英雄机
        // 位置在Game中更新
        // 每帧累积伤害
        frameDamageCount += damagePerFrame;
    }

    /**
     * 激光每帧都可以造成累积的伤害
     */
    public boolean canDamage() {
        if (frameDamageCount >= 30) {  // 每累积30点伤害造成一次判定
            frameDamageCount = 0;  // 重置累积计数
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

