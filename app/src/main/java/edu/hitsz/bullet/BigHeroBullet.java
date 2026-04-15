package edu.hitsz.bullet;

import android.graphics.Bitmap;
import edu.hitsz.application.ImageManager;

/**
 * 大号子弹 - Pro版英雄机专属
 * 命中范围更大
 */
public class BigHeroBullet extends BaseBullet {

    public BigHeroBullet(int locationX, int locationY, int speedX, int speedY, int power) {
        super(locationX, locationY, speedX, speedY, power);
    }

    @Override
    public int getWidth() {return super.getWidth();}

    @Override
    public int getHeight() {
        return super.getHeight();
    }

    @Override
    public Bitmap getImage() {
        return ImageManager.BIG_BULLET_IMAGE;
    }

    @Override
    public int bombEffect() {
        return 0;
    }
}

