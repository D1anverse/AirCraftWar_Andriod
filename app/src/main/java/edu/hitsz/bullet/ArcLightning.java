package edu.hitsz.bullet;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import edu.hitsz.application.ImageManager;

/**
 * 弧状闪电子弹 - Pro版英雄机专属
 * 具有弧形移动轨迹，可穿透敌人
 */
public class ArcLightning extends BaseBullet {

    // 弧形摆动参数
    private float arcPhase;      // 弧形相位
    private float arcAmplitude;  // 弧形幅度
    private float arcFrequency;  // 弧形频率
    private int baseLocationX;   // 基准X坐标

    public ArcLightning(int locationX, int locationY, int speedX, int speedY, int power) {
        super(locationX, locationY, speedX, speedY, power);
        this.baseLocationX = locationX;
        this.arcPhase = 0;
        this.arcAmplitude = 50;  // 横向摆动幅度
        this.arcFrequency = 0.1f; // 摆动频率
    }

    @Override
    public void forward() {
        // 更新相位
        arcPhase += arcFrequency;

        // Y轴正常移动
        locationY += speedY;

        // X轴产生弧形摆动效果
        locationX = (int)(baseLocationX + Math.sin(arcPhase) * arcAmplitude);

        // 判定出界
        if (locationY <= 0) {
            vanish();
        }
    }

    /**
     * 弧状闪电可以穿透敌人
     * 碰撞后不会消失
     */
    @Override
    public void vanish() {
        // 默认不消失，穿透效果
        // 只有出界时才真正消失
    }

    /**
     * 强制消失（出界时调用）
     */
    public void realVanish() {
        super.vanish();
    }

    /**
     * 是否可以穿透
     */
    public boolean canPenetrate() {
        return true;
    }

    @Override
    public int getWidth() {
        return 30;  // 闪电宽度
    }

    @Override
    public int getHeight() {
        return 60;  // 闪电高度
    }

    @Override
    public Bitmap getImage() {
        return ImageManager.ARC_LIGHTNING_IMAGE;
    }

    @Override
    public int bombEffect() {
        return 0;
    }
}
