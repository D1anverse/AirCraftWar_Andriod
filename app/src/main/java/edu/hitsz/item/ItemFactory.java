package edu.hitsz.item;


//道具工厂接口
public interface ItemFactory {
    /**
     * 创建道具
     * @param locationX x坐标
     * @param locationY y坐标
     * @param speedX x方向速度
     * @param speedY y方向速度
     */
    BaseItem createItem(int locationX, int locationY, int speedX, int speedY);
}
