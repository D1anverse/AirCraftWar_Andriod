package edu.hitsz.item;

import edu.hitsz.aircraft.HeroAircraft;
import edu.hitsz.aircraft.HeroType;
import edu.hitsz.strategy.ShootCircle;
import edu.hitsz.strategy.ShootLaser;
import edu.hitsz.strategy.ShootStrategy;

/**
 * 弹道增强道具
 * 不同英雄机类型有不同的弹道效果
 */
public class BulletPlusItem extends BaseItem {

    public BulletPlusItem(int locationX, int locationY, int speedX, int speedY) {
        super(locationX, locationY, speedX, speedY);
    }

    @Override
    public void activateEffect(HeroAircraft heroAircraft) {
        HeroType heroType = heroAircraft.getHeroType();
        ShootStrategy strategy = createStrategyForType(heroType);

        heroAircraft.setStrategy(strategy);
        heroAircraft.saveInterval();
        heroAircraft.setShootInterval(getShootInterval(heroType));
        heroAircraft.isReset = false;
        heroAircraft.setEffectTimer(getEffectDuration(heroType));
    }

    private ShootStrategy createStrategyForType(HeroType heroType) {
        switch (heroType) {
            case HERO_PROMAX:
                // 旗舰型：触发激光模式
                return new ShootLaser();
            case HERO_PRO:
            case HERO_BASIC:
            default:
                // 基础型和进阶型：环形射击
                return new ShootCircle();
        }
    }

    private int getShootInterval(HeroType heroType) {
        switch (heroType) {
            case HERO_PROMAX:
                return 0; // 激光不需要射击间隔
            case HERO_PRO:
                return 1000; // 进阶型较快
            case HERO_BASIC:
            default:
                return 1500;
        }
    }

    private int getEffectDuration(HeroType heroType) {
        switch (heroType) {
            case HERO_PROMAX:
                return 8000;
            case HERO_PRO:
                return 12000;
            case HERO_BASIC:
            default:
                return 10000;
        }
    }
}