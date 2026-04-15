package edu.hitsz.aircraft;


import edu.hitsz.application.GameConfig;
import edu.hitsz.bullet.BulletFactory;
import edu.hitsz.item.*;
import edu.hitsz.strategy.ShootStrategy;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public abstract class AbstractEnemy extends AbstractAircraft {
    protected int itemAmount;

    public AbstractEnemy(int locationX, int locationY, int speedX, int speedY, int hp, ShootStrategy strategy, BulletFactory bulletFactory) {
        super(locationX, locationY, speedX, speedY, hp, strategy, bulletFactory);
    }

    public List<BaseItem> spawnItems(){
        List<BaseItem> itemList = new LinkedList<>();
        Random random = new Random();

        // 随机决定掉落道具的数量（0-3个）
        int dropCount = random.nextInt(itemAmount); // 生成0-3的随机数

        for (int i = 0; i < dropCount; i++) {
            double dropType = random.nextDouble();

            int x = this.getLocationX();
            int y = this.getLocationY() + direction * 2;
            int speedX = 0;
            int speedY = 5;
            BaseItem item;
            ItemFactory factory;

            //概率掉落各种道具或者直接不掉落
            if (dropType < 0.6) {
                factory = new HealthItemFactory();
                item = factory.createItem(x, y, speedX, speedY);
            } else if (dropType < 0.7) {
                factory = new BombItemFactory();
                item = factory.createItem(x, y, speedX, speedY);
            } else if  (dropType < 0.85) {
                factory = new AttackItemFactory();
                item = factory.createItem(x, y, speedX, speedY);
            }else{
                factory = new BulletPlusItemFactory();
                item = factory.createItem(x, y, speedX, speedY);
            }
            itemList.add(item);
        }

        return itemList;
    }

    @Override
    public void forward() {
        super.forward();
        int screenHeight = GameConfig.getInstance().getScreenHeight();
        // 判定 y 轴   向下飞行出界
        if (locationY >= screenHeight) {
            vanish();
        }
    }

    public abstract int getScores();
}
