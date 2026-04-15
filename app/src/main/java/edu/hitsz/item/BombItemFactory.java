package edu.hitsz.item;

//炸弹道具工厂
public class BombItemFactory implements ItemFactory {
    @Override
    public BaseItem createItem(int locationX, int locationY, int speedX, int speedY) {
        return new BombItem(locationX, locationY, speedX, speedY);
    }
}