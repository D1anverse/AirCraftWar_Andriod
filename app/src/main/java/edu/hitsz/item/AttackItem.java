package edu.hitsz.item;

import edu.hitsz.aircraft.HeroAircraft;
import edu.hitsz.aircraft.HeroType;
import edu.hitsz.strategy.ShootLaser;
import edu.hitsz.strategy.ShootMultiStraight;
import edu.hitsz.strategy.ShootStrategy;

/**
 * 火力道具类
 * 不同英雄机类型有不同的火力增强效果
 */
public class AttackItem extends BaseItem {

    public AttackItem(int locationX, int locationY, int speedX, int speedY) {
        super(locationX, locationY, speedX, speedY);
    }

    @Override
    public void activateEffect(HeroAircraft heroAircraft) {
        HeroType heroType = heroAircraft.getHeroType();
        ShootStrategy strategy = createStrategyForType(heroType);

        heroAircraft.setStrategy(strategy);
        heroAircraft.isReset = false;
        heroAircraft.saveInterval();
        heroAircraft.setShootInterval(0);
        heroAircraft.setEffectTimer(getEffectDuration(heroType));
    }

    /**
     * 根据英雄机类型创建对应的射击策略
     */
    private ShootStrategy createStrategyForType(HeroType heroType) {
        switch (heroType) {
            case HERO_PROMAX:
                // 旗舰型：触发激光模式
                return new ShootLaser();
            case HERO_PRO:
            case HERO_BASIC:
            default:
                // 基础型和进阶型：多重直线射击
                return new ShootMultiStraight();
        }
    }

    /**
     * 根据英雄机类型获取效果持续时间
     */
    private int getEffectDuration(HeroType heroType) {
        switch (heroType) {
            case HERO_PROMAX:
                return 8000; // 旗舰型激光持续8秒
            case HERO_PRO:
                return 12000; // 进阶型持续12秒
            case HERO_BASIC:
            default:
                return 10000; // 基础型持续10秒
        }
    }
}