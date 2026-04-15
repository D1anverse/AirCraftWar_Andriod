package edu.hitsz.item;


import edu.hitsz.aircraft.HeroAircraft;
import edu.hitsz.aircraft.HeroType;

/**
 * 回血道具类
 * 拾取后恢复英雄机的生命值
 * 不同英雄机类型有不同的回血效果
 */
public class HealthItem extends BaseItem {

    // 基础回血量
    private static final int BASE_HEAL_AMOUNT = 20;

    public HealthItem(int locationX, int locationY, int speedX, int speedY) {
        super(locationX, locationY, speedX, speedY);
    }

    @Override
    public void activateEffect(HeroAircraft heroAircraft) {
        HeroType heroType = heroAircraft.getHeroType();

        switch (heroType) {
            case HERO_PRO:
                // Pro版：额外回血
                heroAircraft.heal((int)(BASE_HEAL_AMOUNT * 1.5)); // 30点
                break;
            case HERO_PROMAX:
                // ProMax版：攻击力提升
                heroAircraft.increasePower(15); // 攻击力+15
                break;
            case HERO_BASIC:
            default:
                // 基础型：正常回血
                heroAircraft.heal(BASE_HEAL_AMOUNT); // 20点
                break;
        }
    }

}