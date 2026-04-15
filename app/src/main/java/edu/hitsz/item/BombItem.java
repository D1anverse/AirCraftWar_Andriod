package edu.hitsz.item;

import edu.hitsz.aircraft.HeroAircraft;


/**
 * 炸弹道具类
 */
public class BombItem extends BaseItem {


    public BombItem(int locationX, int locationY, int speedX, int speedY) {
        super(locationX, locationY, speedX, speedY);
    }


    @Override
    public void activateEffect(HeroAircraft heroAircraft) {
        heroAircraft.useBomb();
    }

}