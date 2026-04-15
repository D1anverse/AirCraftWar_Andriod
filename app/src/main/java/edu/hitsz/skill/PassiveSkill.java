package edu.hitsz.skill;

import edu.hitsz.aircraft.HeroAircraft;

/**
 * 被动技能基类
 */
public abstract class PassiveSkill implements Skill {
    protected HeroAircraft hero;
    protected boolean isActive = true;

    public void bindHero(HeroAircraft hero) {
        this.hero = hero;
    }

    /**
     * 每帧更新
     */
    public void onUpdate(int timeInterval) {}

    /**
     * 当受到伤害时触发
     * @return 实际受到的伤害
     */
    public int onDamageTaken(int damage) {
        return damage;
    }
}
