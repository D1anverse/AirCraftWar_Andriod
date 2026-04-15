package edu.hitsz.item;

//回血道具工厂
public class HealthItemFactory implements ItemFactory {
    @Override
    public BaseItem createItem(int locationX, int locationY, int speedX, int speedY) {
        return new HealthItem(locationX, locationY, speedX, speedY);
    }
}