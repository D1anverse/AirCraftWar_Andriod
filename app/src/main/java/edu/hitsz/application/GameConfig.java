package edu.hitsz.application;

import android.content.Context;
import android.util.DisplayMetrics;
import edu.hitsz.R;
import edu.hitsz.aircraft.HeroType;

public class GameConfig {
    private static GameConfig instance;
    private int screenWidth;
    private int screenHeight;

    private HeroType selectedHeroType = HeroType.HERO_PRO;

    private GameConfig(Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
    }

    public static void init(Context context) {
        if (instance == null) {
            instance = new GameConfig(context.getApplicationContext());
        }
    }

    public static GameConfig getInstance() {
        return instance;
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    public void setSelectedHeroType(HeroType heroType) {
        this.selectedHeroType = heroType;
    }

    public HeroType getSelectedHeroType() {
        return selectedHeroType;
    }

    public void setSelectedHeroTypeById(String typeId) {
        this.selectedHeroType = HeroType.fromTypeId(typeId);
    }
}