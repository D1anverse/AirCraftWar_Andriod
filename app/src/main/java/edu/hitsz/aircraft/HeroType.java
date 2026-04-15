package edu.hitsz.aircraft;

import android.graphics.Bitmap;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import edu.hitsz.application.ImageManager;
import edu.hitsz.bullet.BigBulletFactory;
import edu.hitsz.bullet.BulletFactory;
import edu.hitsz.bullet.HeroBulletFactory;
import edu.hitsz.skill.ActiveSkill;
import edu.hitsz.skill.PassiveSkill;
import edu.hitsz.strategy.ShootDoubleWing;
import edu.hitsz.strategy.ShootStrategy;
import edu.hitsz.strategy.ShootStraight;
import edu.hitsz.skill.active.ArcLightningSkill;
import edu.hitsz.skill.active.ShieldSkill;

/**
 * 英雄机类型枚举 - 定义不同英雄机的属性和行为
 * 扩展新英雄机只需在此添加新的枚举值
 */
public enum HeroType {

    HERO_BASIC("基础型", "hero", 100, 30, 0) {
        @Override
        public Bitmap getImage() {
            return ImageManager.HERO_IMAGE;
        }

        @Override
        public ShootStrategy createDefaultStrategy() {
            return new ShootStraight();
        }

        @Override
        public BulletFactory createBulletFactory() {
            return new HeroBulletFactory();
        }

        @Override
        public String getDescription() {
            return "基础型号\n生命值: 100\n攻击力: 30\n特点: 均衡型";
        }

        @Override
        public List<PassiveSkill> createPassiveSkills() {
            return Collections.emptyList();
        }

        @Override
        public ActiveSkill createActiveSkill() {
            return null;
        }
    },

    HERO_PRO("进阶型", "hero_pro", 150, 40, 0) {
        @Override
        public Bitmap getImage() {
            return ImageManager.HERO_PRO;
        }

        @Override
        public ShootStrategy createDefaultStrategy() {
            return new ShootStraight();
        }

        @Override
        public BulletFactory createBulletFactory() {
            return new BigBulletFactory(); // 大号子弹，命中范围更大
        }

        @Override
        public String getDescription() {
            return "进阶型号\n生命值: 150\n攻击力: 40\n特点: 大范围子弹，增强回血，弧状闪电技能";
        }

        @Override
        public List<PassiveSkill> createPassiveSkills() {
            return Collections.emptyList(); // 道具效果由道具类处理
        }

        @Override
        public ActiveSkill createActiveSkill() {
            return new ArcLightningSkill(80);
        }
    },

    HERO_PROMAX("旗舰型", "hero_promax", 200, 50, 0) {
        @Override
        public Bitmap getImage() {
            return ImageManager.HERO_PROMAX;
        }

        @Override
        public ShootStrategy createDefaultStrategy() {
            return new ShootDoubleWing(); // 双翼射击
        }

        @Override
        public BulletFactory createBulletFactory() {
            return new HeroBulletFactory();
        }

        @Override
        public String getDescription() {
            return "旗舰型号\n生命值: 200\n攻击力: 50\n特点: 双翼射击，激光模式，护盾技能";
        }

        @Override
        public List<PassiveSkill> createPassiveSkills() {
            return Collections.emptyList(); // 道具效果由道具类处理
        }

        @Override
        public ActiveSkill createActiveSkill() {
            return new ShieldSkill();
        }
    };

    // 基础属性
    private final String displayName;
    private final String typeId;
    private final int baseHp;
    private final int basePower;
    private final int baseShootInterval;

    HeroType(String displayName, String typeId, int baseHp, int basePower, int baseShootInterval) {
        this.displayName = displayName;
        this.typeId = typeId;
        this.baseHp = baseHp;
        this.basePower = basePower;
        this.baseShootInterval = baseShootInterval;
    }

    // 抽象方法 - 子类必须实现
    public abstract Bitmap getImage();
    public abstract ShootStrategy createDefaultStrategy();
    public abstract BulletFactory createBulletFactory();
    public abstract String getDescription();
    public abstract List<PassiveSkill> createPassiveSkills();
    public abstract ActiveSkill createActiveSkill();

    // Getter 方法
    public String getDisplayName() { return displayName; }
    public String getTypeId() { return typeId; }
    public int getBaseHp() { return baseHp; }
    public int getBasePower() { return basePower; }
    public int getBaseShootInterval() { return baseShootInterval; }

    // 根据 typeId 获取英雄机类型
    public static HeroType fromTypeId(String typeId) {
        for (HeroType type : values()) {
            if (type.typeId.equals(typeId)) {
                return type;
            }
        }
        return HERO_PRO; // 默认返回进阶型
    }
}