package edu.hitsz.skill.active;

import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.skill.ActiveSkill;
import java.util.Collections;
import java.util.List;

/**
 * 保护罩技能 - ProMax版英雄机专属
 * 开启后一段时间内无敌
 */
public class ShieldSkill extends ActiveSkill {

    public ShieldSkill() {
        super(100, 5, 30000, 5000); // 100能量，每秒恢复5，30秒冷却，持续5秒
    }

    @Override
    public String getName() {
        return "能量护盾";
    }

    @Override
    public String getDescription() {
        return "开启保护罩，持续5秒无敌";
    }

    @Override
    public List<BaseBullet> use() {
        if (!canUse()) return Collections.emptyList();

        onStart();
        hero.setInvincible(true);
        return Collections.emptyList();
    }

    @Override
    protected void onEnd() {
        super.onEnd();
        if (hero != null) {
            hero.setInvincible(false);
        }
    }
}

