package edu.hitsz.item;

//攻击道具工厂

public class AttackItemFactory implements ItemFactory {
    @Override
    public BaseItem createItem(int locationX, int locationY, int speedX, int speedY) {
        return new AttackItem(locationX, locationY, speedX, speedY);
    }
}