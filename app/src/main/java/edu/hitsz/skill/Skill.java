package edu.hitsz.skill;

public interface Skill {
    /**
     * 获取技能名称
     */
    String getName();

    /**
     * 获取技能描述
     */
    String getDescription();

    /**
     * 获取技能图标ID（可选）
     */
    default int getIconResId() { return 0; }
}
