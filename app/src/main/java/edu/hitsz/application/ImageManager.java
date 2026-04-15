package edu.hitsz.application;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.util.HashMap;
import java.util.Map;

import edu.hitsz.aircraft.*;
import edu.hitsz.bullet.BigHeroBullet;
import edu.hitsz.bullet.EnemyBullet;
import edu.hitsz.bullet.HeroBullet;
import edu.hitsz.item.*;
import edu.hitsz.R;
import edu.hitsz.skill.active.ArcLightningSkill;
import edu.hitsz.skill.active.ShieldSkill;
import edu.hitsz.strategy.ShootLaser;

public class ImageManager {
    private static final Map<String, Bitmap> CLASSNAME_IMAGE_MAP = new HashMap<>();

    public static Bitmap BACKGROUND_IMAGE_EASY;
    public static Bitmap BACKGROUND_IMAGE_NORMAL;
    public static Bitmap BACKGROUND_IMAGE_HARD;
    public static Bitmap HERO_IMAGE;
    public static Bitmap HERO_BULLET_IMAGE;
    public static Bitmap ENEMY_BULLET_IMAGE;
    public static Bitmap MOB_ENEMY_IMAGE;
    public static Bitmap Elite_ENEMY_IMAGE;
    public static Bitmap HEALTH_ITEM_IMAGE;
    public static Bitmap BOMB_ITEM_IMAGE;
    public static Bitmap ATTACK_ITEM_IMAGE;
    public static Bitmap SUPER_ELITE_IMAGE;
    public static Bitmap BOSS_IMAGE;
    public static Bitmap BULLET_PLUS_ITEM_IMAGE;

    public static Bitmap ARC_LIGHTNING_IMAGE;

    public static Bitmap SHIELD_IMAGE;

    public static Bitmap BIG_BULLET_IMAGE;

    public static Bitmap LASER_IMAGE;

    public static Bitmap HERO_PRO;

    public static Bitmap HERO_PROMAX;

    public static void init(Resources res) {
        BACKGROUND_IMAGE_EASY = BitmapFactory.decodeResource(res, R.drawable.bg);
        BACKGROUND_IMAGE_NORMAL = BitmapFactory.decodeResource(res, R.drawable.bg2);
        BACKGROUND_IMAGE_HARD = BitmapFactory.decodeResource(res, R.drawable.bg5);
        HERO_IMAGE = BitmapFactory.decodeResource(res, R.drawable.hero);
        MOB_ENEMY_IMAGE = BitmapFactory.decodeResource(res, R.drawable.mob);
        HERO_BULLET_IMAGE = BitmapFactory.decodeResource(res, R.drawable.bullet_hero);
        ENEMY_BULLET_IMAGE = BitmapFactory.decodeResource(res, R.drawable.bullet_enemy);
        Elite_ENEMY_IMAGE = BitmapFactory.decodeResource(res, R.drawable.elite);
        HEALTH_ITEM_IMAGE = BitmapFactory.decodeResource(res, R.drawable.prop_blood);
        BOMB_ITEM_IMAGE = BitmapFactory.decodeResource(res, R.drawable.prop_bomb);
        ATTACK_ITEM_IMAGE = BitmapFactory.decodeResource(res, R.drawable.prop_bullet);
        SUPER_ELITE_IMAGE = BitmapFactory.decodeResource(res, R.drawable.eliteplus);
        BOSS_IMAGE = BitmapFactory.decodeResource(res, R.drawable.boss);
        BULLET_PLUS_ITEM_IMAGE = BitmapFactory.decodeResource(res, R.drawable.prop_bulletplus);
        HERO_PRO = BitmapFactory.decodeResource(res, R.drawable.hero_aircraft_pro);
        HERO_PROMAX = BitmapFactory.decodeResource(res, R.drawable.hero_aircraft_promax);
        BIG_BULLET_IMAGE = BitmapFactory.decodeResource(res, R.drawable.big_bullet);
        LASER_IMAGE = BitmapFactory.decodeResource(res, R.drawable.laser);
        ARC_LIGHTNING_IMAGE = BitmapFactory.decodeResource(res, R.drawable.arc_lightning);
        SHIELD_IMAGE = BitmapFactory.decodeResource(res, R.drawable.shield);


        CLASSNAME_IMAGE_MAP.put(HeroAircraft.class.getName(), HERO_PRO);
        CLASSNAME_IMAGE_MAP.put(MobEnemy.class.getName(), MOB_ENEMY_IMAGE);
        CLASSNAME_IMAGE_MAP.put(HeroBullet.class.getName(), HERO_BULLET_IMAGE);
        CLASSNAME_IMAGE_MAP.put(EnemyBullet.class.getName(), ENEMY_BULLET_IMAGE);
        CLASSNAME_IMAGE_MAP.put(EliteEnemy.class.getName(), Elite_ENEMY_IMAGE);
        CLASSNAME_IMAGE_MAP.put(HealthItem.class.getName(), HEALTH_ITEM_IMAGE);
        CLASSNAME_IMAGE_MAP.put(BombItem.class.getName(), BOMB_ITEM_IMAGE);
        CLASSNAME_IMAGE_MAP.put(AttackItem.class.getName(), ATTACK_ITEM_IMAGE);
        CLASSNAME_IMAGE_MAP.put(SuperElite.class.getName(), SUPER_ELITE_IMAGE);
        CLASSNAME_IMAGE_MAP.put(BossEnemy.class.getName(), BOSS_IMAGE);
        CLASSNAME_IMAGE_MAP.put(BulletPlusItem.class.getName(), BULLET_PLUS_ITEM_IMAGE);
        CLASSNAME_IMAGE_MAP.put(ArcLightningSkill.class.getName(), ARC_LIGHTNING_IMAGE);
        CLASSNAME_IMAGE_MAP.put(ShieldSkill.class.getName(), SHIELD_IMAGE);
        CLASSNAME_IMAGE_MAP.put(BigHeroBullet.class.getName(), BIG_BULLET_IMAGE);
        CLASSNAME_IMAGE_MAP.put(ShootLaser.class.getName(), LASER_IMAGE);
    }

    public static Bitmap get(String className) {
        return CLASSNAME_IMAGE_MAP.get(className);
    }

    public static Bitmap get(Object obj) {
        return obj == null ? null : get(obj.getClass().getName());
    }
}