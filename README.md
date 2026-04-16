# 雷霆战机 - Aircraft War

一款基于 Android 平台开发的飞行射击游戏，采用传统的面向对象设计模式构建。

## 项目概述

本项目是一个 Android 平台的飞行射击游戏，玩家控制战机击败敌机获取分数，支持多种难度模式和特色英雄机类型。

---

## 代码结构

```
edu.hitsz/
├── aircraft/          # 飞机相关类
│   ├── AbstractAircraft.java      # 飞机抽象基类
│   ├── AbstractEnemy.java         # 敌机抽象基类
│   ├── HeroAircraft.java          # 英雄机类（单例模式）
│   ├── HeroType.java              # 英雄机类型枚举
│   ├── BossEnemy.java             # Boss敌机
│   ├── EliteEnemy.java            # 精英敌机
│   ├── MobEnemy.java              # 普通敌机
│   ├── SuperElite.java            # 超级精英敌机
│   └── *Factory.java              # 各种工厂类
│
├── basic/             # 基础类
│   └── AbstractFlyingObject.java   # 可飞行对象基类（位置、速度、碰撞检测）
│
├── bullet/            # 子弹相关类
│   ├── BaseBullet.java            # 子弹基类
│   ├── HeroBullet.java            # 英雄机子弹
│   ├── EnemyBullet.java           # 敌机子弹
│   ├── BigHeroBullet.java         # 大型子弹
│   ├── LaserBullet.java           # 激光子弹
│   ├── ArcLightning.java          # 弧状闪电
│   └── *Factory.java              # 子弹工厂
│
├── item/              # 道具相关类
│   ├── BaseItem.java              # 道具基类
│   ├── AttackItem.java            # 火力道具
│   ├── BombItem.java              # 炸弹道具
│   ├── HealthItem.java            # 回血道具
│   ├── BulletPlusItem.java         # 子弹增强道具
│   └── *Factory.java              # 道具工厂
│
├── strategy/          # 射击策略（策略模式）
│   ├── ShootStrategy.java         # 策略接口
│   ├── ShootStraight.java         # 直线射击
│   ├── ShootDoubleWing.java       # 双翼射击
│   ├── ShootMultiStraight.java    # 多重直线射击
│   ├── ShootCircle.java           # 环形射击
│   └── ShootLaser.java            # 激光射击
│
├── skill/             # 技能系统
│   ├── Skill.java                 # 技能接口
│   ├── ActiveSkill.java           # 主动技能基类
│   ├── PassiveSkill.java          # 被动技能基类
│   └── active/
│       ├── ArcLightningSkill.java # 雷霆一击（弧状闪电）
│       └── ShieldSkill.java        # 护盾技能
│
├── application/       # 应用层
│   ├── Game.java                  # 游戏主循环（SurfaceView + Runnable）
│   ├── GameEasy/Normal/Hard.java  # 不同难度实现
│   ├── GameActivity.java           # 游戏Activity
│   ├── MenuActivity.java          # 主菜单Activity
│   ├── HeroSelectActivity.java    # 英雄机选择Activity
│   ├── ScoreActivity.java         # 排行榜Activity
│   ├── ImageManager.java          # 图片资源管理
│   ├── GameConfig.java            # 游戏配置（单例）
│   └── ScoreAdapter.java          # 排行榜适配器
│
├── dao/               # 数据访问层
│   ├── DAO.java                   # 数据访问接口
│   ├── Score.java                 # 分数实体类
│   ├── ScoreDAOImpl.java          # 分数DAO实现
│   └── ScoreDatabaseHelper.java   # SQLite数据库助手
│
└── music/             # 音乐管理
    └── PlaySoundManager.java      # 音效播放管理
```

---

## 核心模块介绍

### 1. 游戏主循环（Game.java）

游戏采用 `SurfaceView` + `Runnable` 模式实现主动刷新：

```java
public class Game extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    @Override
    public void run() {
        while (isRunning) {
            long startTime = System.currentTimeMillis();
            updateGame();   // 更新游戏逻辑
            drawGame();     // 绘制画面
            // 控制帧率（约50fps）
        }
    }
}
```

**核心流程**：
1. 每200ms产生新敌机
2. 执行射击策略
3. 更新所有对象位置
4. 进行碰撞检测
5. 移除无效对象

### 2. 碰撞检测系统（AbstractFlyingObject.java）

采用矩形区域碰撞检测：

```java
public boolean crash(AbstractFlyingObject flyingObject) {
    return x + (fWidth+this.getWidth())/2 > locationX
        && x - (fWidth+this.getWidth())/2 < locationX
        && y + (fHeight/fFactor+this.getHeight()/factor)/2 > locationY
        && y - (fHeight/fFactor+this.getHeight()/factor)/2 < locationY;
}
```

飞机类使用 `factor=2` 缩小纵向碰撞区域，模拟飞机较薄的特性。

### 3. 射击策略系统（策略模式）

```java
public interface ShootStrategy {
    List<BaseBullet> doShoot(int x, int y, int direction, int power, BulletFactory factory);
}

// 不同策略实现不同射击模式
public class ShootDoubleWing implements ShootStrategy {
    @Override
    public List<BaseBullet> doShoot(int x, int y, int direction, int power, BulletFactory factory) {
        // 从飞机两侧发射子弹
        res.add(factory.createBullet(x - 30, y, 0, direction * 10, power));
        res.add(factory.createBullet(x + 30, y, 0, direction * 10, power));
        return res;
    }
}
```

英雄机可通过 `setStrategy()` 动态切换射击模式（如火力道具触发激光射击）。

### 4. 工厂模式

#### 敌机工厂
```java
public interface EnemyFactory {
    AbstractEnemy createEnemy(int hp);
}

public class BossFactory implements EnemyFactory {
    @Override
    public AbstractEnemy createEnemy(int hp) {
        return new BossEnemy(...);
    }
}
```

#### 子弹工厂
```java
public interface BulletFactory {
    BaseBullet createBullet(int x, int y, int speedX, int speedY, int power);
}
```

工厂模式使新增敌机类型或子弹类型时无需修改已有代码。

### 5. 技能系统

主动技能支持能量充能、冷却时间、持续时间：

```java
public class ActiveSkill implements Skill {
    protected float energy;           // 能量值（浮点计算避免除法损失）
    protected long cooldownTime;      // 冷却时间
    protected int duration;           // 持续时间
    protected boolean isActive;       // 是否激活

    public boolean canUse() {
        return cooldownReady && energy >= maxEnergy && !isActive;
    }
}
```

---

## 开发模式分析

### 1. 策略模式（Strategy Pattern）

**应用场景**：射击策略系统
```java
HeroAircraft 持有 ShootStrategy 引用
    ↓
不同策略实现：ShootStraight, ShootDoubleWing, ShootLaser...
```
**优势**：射击行为可动态切换，无需修改飞机类

### 2. 工厂模式（Factory Pattern）

**应用场景**：敌机生成、子弹创建
```java
AbstractEnemy enemy = new EliteEnemyFactory().createEnemy(hp);
BaseBullet bullet = new HeroBulletFactory().createBullet(...);
```
**优势**：解耦对象创建过程，便于扩展新类型

### 3. 观察者模式（Observer Pattern）

**应用场景**：炸弹道具全屏伤害
```java
// HeroAircraft 持有观察者列表
public void useBomb() {
    for (AbstractFlyingObject observer : observerList) {
        tempScores += observer.bombEffect();
    }
}
```
**优势**：道具效果可广播给所有观察者

### 4. 模板方法模式（Template Method）

**应用场景**：不同难度的游戏类继承 Game
```java
abstract class Game {
    abstract protected void increaseDifficulty();  // 子类实现
    // 其他逻辑由父类控制
}
```
**优势**：公共逻辑复用，差异部分由子类实现

### 5. 单例模式（Singleton Pattern）

**应用场景**：英雄机、游戏配置
```java
private static final HeroAircraft instance = new HeroAircraft();
public static HeroAircraft getInstance() { return instance; }
```
**优势**：全局唯一实例，避免多实例冲突

---

## Android 与 PC 开发的差异

本项目原本为 Java PC 游戏，后迁移至 Android 平台，主要差异如下：

### 1. 图形渲染

| 方面 | PC版（Java Swing） | Android版 |
|------|-------------------|----------|
| 渲染方式 | `Graphics.drawImage()` | `SurfaceView` + `Canvas` |
| 刷新机制 | 被动 repaint | 主动 gameLoop 线程 |
| 双缓冲 | 自动 | 需手动处理 |

```java
// Android: 主动刷新
public void run() {
    while (isRunning) {
        Canvas canvas = holder.lockCanvas();
        if (canvas != null) {
            drawGame(canvas);
            holder.unlockCanvasAndPost(canvas);
        }
    }
}
```

### 2. 用户输入

| 方面 | PC版 | Android版 |
|------|------|----------|
| 英雄机控制 | 键盘方向键/WASD | 触摸拖动 |
| 技能触发 | 键盘快捷键 | 点击技能按钮 |
| 射击控制 | 空格键/自动射击 | 自动射击 |

```java
// Android: 触摸事件处理
@Override
public boolean onTouchEvent(MotionEvent event) {
    int x = (int) event.getX();
    int y = (int) event.getY();

    if (event.getAction() == MotionEvent.ACTION_DOWN ||
        event.getAction() == MotionEvent.ACTION_MOVE) {
        // 检查技能按钮区域
        if (isInSkillArea(x, y)) {
            triggerActiveSkill();
        } else {
            heroAircraft.setLocation(x, y);  // 移动英雄机
        }
    }
    return true;
}
```

### 3. 数据持久化

| 方面 | PC版 | Android版 |
|------|------|----------|
| 存储方式 | 文件IO/Properties | SQLite数据库 |
| 排行榜 | 文本文件 | SQLite多表 |

```java
// Android: SQLite 数据库
public class ScoreDatabaseHelper extends SQLiteOpenHelper {
    // 三个难度分别对应三个表
    public static final String TABLE_EASY = "scores_easy";
    public static final String TABLE_NORMAL = "scores_normal";
    public static final String TABLE_HARD = "scores_hard";
}
```

### 4. 生命周期

| 方面 | PC版 | Android版 |
|------|------|----------|
| 应用状态 | 单一状态 | Activity 生命周期 |
| 暂停/恢复 | 无需处理 | `onPause()` / `onResume()` |
| 资源释放 | 退出即释放 | 需显式管理 |

```java
// Android: 生命周期管理
@Override
protected void onPause() {
    super.onPause();
    if (gameView != null) {
        gameView.pauseGame();  // 暂停背景音乐
    }
}
```

### 5. 音效播放

| 方面 | PC版 | Android版 |
|------|------|----------|
| API | Java Sound API | Android MediaPlayer |
| 资源格式 | WAV/MP3 | MP3/OGG |

```java
// Android: 使用 MediaPlayer
private MediaPlayer bgmPlayer;
public void playBgm() {
    bgmPlayer = MediaPlayer.create(context, R.raw.bgm);
    bgmPlayer.setLooping(true);
    bgmPlayer.start();
}
```

---

## 项目配置

- **Min SDK**: 30 (Android 11)
- **Target SDK**: 36
- **Java Version**: 11
- **构建工具**: Gradle

### 主要依赖
- AndroidX AppCompat
- Material Design Components
- AndroidX Activity
- ConstraintLayout

---

## 运行说明

1. 使用 Android Studio 打开项目
2. 连接 Android 设备或启动模拟器
3. 点击运行按钮

---

## License

MIT License
