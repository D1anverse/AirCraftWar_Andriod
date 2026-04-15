package edu.hitsz.bullet;

import edu.hitsz.basic.AbstractFlyingObject;
import edu.hitsz.application.GameConfig;


/**
 * 子弹类。
 * 也可以考虑不同类型的子弹
 *
 * @author hitsz
 */
public abstract class BaseBullet extends AbstractFlyingObject {

    protected int power;

    int screenWidth = GameConfig.getInstance().getScreenWidth();
    int screenHeight = GameConfig.getInstance().getScreenHeight();

    public BaseBullet(int locationX, int locationY, int speedX, int speedY, int power) {
        super(locationX, locationY, speedX, speedY);
        this.power = power;
    }

    @Override
    public void forward() {
        locationX += speedX;
        locationY += speedY;
        if (locationX <= 0 || locationX >= screenWidth) {
            // 横向超出边界后反向
            speedX = -speedX;
        }

        // 判定 x 轴出界
        if (locationX <= 0 || locationX >= screenWidth) {
            vanish();
        }

        // 判定 y 轴出界
        if (speedY > 0 && locationY >= screenHeight ) {
            // 向下飞行出界
            vanish();
        }else if (locationY <= 0){
            // 向上飞行出界
            vanish();
        }
    }

    public int getPower() {
        return power;
    }
}
