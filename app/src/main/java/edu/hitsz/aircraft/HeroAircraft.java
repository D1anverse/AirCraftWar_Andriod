package edu.hitsz.aircraft;

import edu.hitsz.application.GameConfig;
import edu.hitsz.application.ImageManager;
import edu.hitsz.basic.AbstractFlyingObject;
import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.bullet.HeroBulletFactory;
import edu.hitsz.item.BaseItem;
import edu.hitsz.skill.ActiveSkill;
import edu.hitsz.skill.PassiveSkill;
import edu.hitsz.strategy.ShootStraight;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class HeroAircraft extends AbstractAircraft {
    private int effectTimer;
    public boolean isReset = true;
    private static final HeroAircraft instance = new HeroAircraft();

    private final List<AbstractFlyingObject> observerList = new LinkedList<>();
    private int tempScores = 0;
    private long savedShootInterval;

    private HeroType heroType;

    // 技能系统
    private List<PassiveSkill> passiveSkills = new ArrayList<>();
    private ActiveSkill activeSkill;

    // 无敌状态
    private boolean isInvincible = false;

    // 私有构造函数
    private HeroAircraft() {
        super(
                GameConfig.getInstance().getScreenWidth() / 2,
                GameConfig.getInstance().getScreenHeight() - getHeroImageHeight(),
                0, 0,
                getHeroTypeStatic().getBaseHp(),
                getHeroTypeStatic().createDefaultStrategy(),
                getHeroTypeStatic().createBulletFactory()
        );
        this.heroType = getHeroTypeStatic();
        this.shootInterval = heroType.getBaseShootInterval();
        this.power = heroType.getBasePower();
        this.direction = -1;
    }

    private static HeroType getHeroTypeStatic() {
        GameConfig config = GameConfig.getInstance();
        return config != null ? config.getSelectedHeroType() : HeroType.HERO_PRO;
    }

    private static int getHeroImageHeight() {
        HeroType type = getHeroTypeStatic();
        return type.getImage() != null ? type.getImage().getHeight() : 0;
    }

    public static HeroAircraft getInstance() {
        return instance;
    }

    public void setHeroType(HeroType heroType) {
        this.heroType = heroType;
    }

    public HeroType getHeroType() {
        return heroType;
    }

    public android.graphics.Bitmap getHeroImage() {
        return heroType.getImage();
    }

    public void init(int locationX, int locationY, int speedX, int speedY, int hp) {
        this.heroType = GameConfig.getInstance().getSelectedHeroType();

        this.locationX = locationX;
        this.locationY = locationY;
        this.speedX = speedX;
        this.speedY = speedY;
        this.hp = hp;
        this.maxHp = hp;
        this.isValid = true;
        this.effectTimer = 0;
        this.isReset = true;
        this.shootInterval = heroType.getBaseShootInterval();
        this.power = heroType.getBasePower();

        setStrategy(heroType.createDefaultStrategy());

        // 初始化技能
        this.passiveSkills = heroType.createPassiveSkills();
        this.activeSkill = heroType.createActiveSkill();

        for (PassiveSkill skill : passiveSkills) {
            skill.bindHero(this);
        }
        if (activeSkill != null) {
            activeSkill.bindHero(this);
        }
    }

    // 无敌状态
    public void setInvincible(boolean invincible) {
        this.isInvincible = invincible;
    }

    public boolean isInvincible() {
        return isInvincible;
    }

    // 攻击力提升
    public void increasePower(int amount) {
        this.power += amount;
    }

    // 技能更新
    public void updateSkills(int timeInterval) {
        for (PassiveSkill skill : passiveSkills) {
            skill.onUpdate(timeInterval);
        }
        if (activeSkill != null) {
            activeSkill.onUpdate(timeInterval);
        }
    }

    // 使用主动技能
    public List<BaseBullet> useActiveSkill() {
        if (activeSkill != null && activeSkill.canUse()) {
            return activeSkill.use();
        }
        return Collections.emptyList();
    }

    // 受伤处理
    @Override
    public void decreaseHp(int decrement) {
        int actualDamage = decrement;
        if (isInvincible) {
            actualDamage = 0;
        }
        super.decreaseHp(actualDamage);
    }

    @Override
    public void forward() {
        // 英雄机由触摸控制
    }

    public void heal(int healAmount) {
        this.hp = Math.min(this.hp + healAmount, maxHp);
    }

    public void setShootInterval(int interval) {
        this.shootInterval = interval;
    }

    public void setEffectTimer(int duration) {
        this.effectTimer = duration;
    }

    public long getEffectTimer() {
        return this.effectTimer;
    }

    public void resetStrategy() {
        this.strategy = heroType.createDefaultStrategy();
        this.shootInterval = savedShootInterval;
        this.isReset = true;
    }

    public void effectTimerUpdate(int timeInterval) {
        this.effectTimer -= timeInterval;
    }

    public void addObserver(AbstractFlyingObject observer) {
        observerList.add(observer);
    }

    public void removeObserver(AbstractFlyingObject observer) {
        observerList.remove(observer);
    }

    public void removeInvalid() {
        observerList.removeIf(AbstractFlyingObject::notValid);
    }

    public void useBomb() {
        tempScores = 0;
        for (AbstractFlyingObject observer : observerList) {
            tempScores += observer.bombEffect();
        }
    }

    public int getScores() {
        int score = tempScores;
        tempScores = 0;
        return score;
    }

    public void addInterval(int interval) {
        this.shootInterval += interval;
    }

    public void saveInterval() {
        this.savedShootInterval = this.shootInterval;
    }

    public long getInterval() {
        return this.shootInterval;
    }

    // Getter
    public ActiveSkill getActiveSkill() { return activeSkill; }
    public List<PassiveSkill> getPassiveSkills() { return passiveSkills; }

    @Override
    public int bombEffect() {
        return 0;
    }
}
