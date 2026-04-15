package edu.hitsz.skill.active;

import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.bullet.ArcLightning;
import edu.hitsz.skill.ActiveSkill;
import java.util.LinkedList;
import java.util.List;

/**
 * 弧状闪电技能 - Pro版英雄机专属
 * 发射弧状闪电，对敌人造成伤害
 */
public class ArcLightningSkill extends ActiveSkill {
    private int lightningPower;

    public ArcLightningSkill(int lightningPower) {
        super(80, 8, 15000, 3000); // 80能量，每秒恢复8，15秒冷却，持续3秒
        this.lightningPower = lightningPower * 3;
    }

    @Override
    public String getName() {
        return "雷霆一击";
    }

    @Override
    public String getDescription() {
        return "发射弧状闪电，穿透敌人造成伤害";
    }

    @Override
    public List<BaseBullet> use() {
        if (!canUse()) return new LinkedList<>();

        onStart();

        List<BaseBullet> bullets = new LinkedList<>();

        // 生成5道闪电,形成扇形攻击范围
        int lightningCount = 5;
        for (int i = 0; i < lightningCount; i++) {
            ArcLightning lightning = new ArcLightning(
                    hero.getLocationX() + (i - 2) * 30,  // 横向分布
                    hero.getLocationY() - hero.getHeight() / 2,
                    0, -15,  // 向上飞行
                    lightningPower
            );
            bullets.add(lightning);
        }

        return bullets;
    }
}

