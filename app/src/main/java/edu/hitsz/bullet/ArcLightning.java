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

    private float arcPhase;
    private float arcAmplitude;
    private float arcFrequency;
    private int baseLocationX;
    private int collisionWidth;

    public ArcLightning(int locationX, int locationY, int speedX, int speedY, int power) {
        super(locationX, locationY, speedX, speedY, power);
        this.baseLocationX = locationX;
        this.arcPhase = 0;
        this.arcAmplitude = 50;
        this.arcFrequency = 0.1f;
        this.collisionWidth = 450;
    }

    @Override
    public void forward() {
        arcPhase += arcFrequency;
        locationY += speedY;
        locationX = (int)(baseLocationX + Math.sin(arcPhase) * arcAmplitude);

        if (locationY <= 0) {
            realVanish();
        }
    }

    @Override
    public void vanish() {
    }

    public void realVanish() {
        super.vanish();
    }

    public boolean canPenetrate() {
        return true;
    }

    @Override
    public int getWidth() {
        return collisionWidth;
    }

    @Override
    public int getHeight() {
        return 60;
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
