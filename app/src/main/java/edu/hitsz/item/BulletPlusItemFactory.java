package edu.hitsz.item;

public class BulletPlusItemFactory implements ItemFactory{

    @Override
    public BaseItem createItem(int locationX, int locationY, int speedX, int speedY) {
        return new BulletPlusItem(locationX, locationY, speedX, speedY);
    }

}
