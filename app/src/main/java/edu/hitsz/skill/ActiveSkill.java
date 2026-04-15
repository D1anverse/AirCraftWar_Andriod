package edu.hitsz.skill;

import edu.hitsz.aircraft.HeroAircraft;
import java.util.List;
import edu.hitsz.bullet.BaseBullet;

/**
 * 主动技能基类
 * 需要按钮触发，有冷却和能量限制
 */
public abstract class ActiveSkill implements Skill {
    protected HeroAircraft hero;

    // 能量系统
    protected int energy = 0;
    protected int maxEnergy;
    protected int energyRegenRate; // 每秒恢复能量
    protected int EnergyRegenRate;
    protected float energyFraction = 0.0f;

    // 冷却系统
    protected long cooldownTime;    // 冷却时间（毫秒）
    protected long lastUseTime = 0; // 上次使用时间
    protected boolean isOnCooldown = false;

    // 持续时间
    protected int duration;         // 技能持续时间（毫秒）
    protected int remainingDuration = 0;
    protected boolean isActive = false;

    public ActiveSkill(int maxEnergy, int energyRegenRate, long cooldownTime, int duration) {
        this.maxEnergy = maxEnergy;
        this.energyRegenRate = energyRegenRate;
        this.cooldownTime = cooldownTime;
        this.duration = duration;
        this.energy = maxEnergy;
    }

    public void bindHero(HeroAircraft hero) {
        this.hero = hero;
    }

    /**
     * 是否可以使用技能
     */
    public boolean canUse() {
        long currentTime = System.currentTimeMillis();
        boolean cooldownReady = (currentTime - lastUseTime) >= cooldownTime;
        return cooldownReady && energy >= maxEnergy && !isActive;
    }

    /**
     * 使用技能
     * @return 技能产生的子弹列表（如果有）
     */
    public abstract List<BaseBullet> use();

    /**
     * 每帧更新
     */
    public void onUpdate(int timeInterval) {
        // 能量恢复（使用浮点数计算）
        if (!isActive && energy < maxEnergy) {
            // 改进：使用浮点数避免整数除法损失
            float energyGain = (energyRegenRate * timeInterval) / 1000.0f;
            energy += (int)energyGain;

            // 补偿剩余的小数部分
            energyFraction += energyGain - (int)energyGain;
            if (energyFraction >= 1.0f) {
                energy += 1;
                energyFraction -= 1.0f;
            }

            energy = Math.min(energy, maxEnergy);
        }

        // 技能持续时间更新
        if (isActive) {
            remainingDuration -= timeInterval;
            if (remainingDuration <= 0) {
                onEnd();
            }
        }
    }

    /**
     * 技能开始
     */
    protected void onStart() {
        isActive = true;
        remainingDuration = duration;
        energy = 0;
        lastUseTime = System.currentTimeMillis();
    }

    /**
     * 技能结束
     */
    protected void onEnd() {
        isActive = false;
    }

    // Getters
    public int getEnergy() { return energy; }
    public int getMaxEnergy() { return maxEnergy; }
    public float getEnergyPercent() { return (float) energy / maxEnergy; }
    public boolean isActive() { return isActive; }
    public long getRemainingCooldown() {
        long elapsed = System.currentTimeMillis() - lastUseTime;
        return Math.max(0, cooldownTime - elapsed);
    }
}

