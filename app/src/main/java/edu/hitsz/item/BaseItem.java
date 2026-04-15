package edu.hitsz.item;

import edu.hitsz.aircraft.HeroAircraft;
import edu.hitsz.application.GameConfig;
import edu.hitsz.basic.AbstractFlyingObject;

//道具类
public abstract class BaseItem extends AbstractFlyingObject {

    public BaseItem(int locationX, int locationY, int speedX, int speedY) {
        super(locationX, locationY, speedX, speedY);
    }



    public abstract void activateEffect(HeroAircraft heroAircraft);

    @Override
    public void forward() {
        super.forward();

        int screenWidth = GameConfig.getInstance().getScreenWidth();
        int screenHeight = GameConfig.getInstance().getScreenHeight();

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
    public int bombEffect() {
        return 0;
    }
}
