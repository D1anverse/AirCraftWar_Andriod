package edu.hitsz.application;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import edu.hitsz.R;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import edu.hitsz.aircraft.*;
import edu.hitsz.basic.AbstractFlyingObject;
import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.item.BaseItem;
import edu.hitsz.item.BombItem;
import edu.hitsz.music.PlaySoundManager; // 需修改为Android版
import edu.hitsz.bullet.ArcLightning;
import edu.hitsz.bullet.LaserBullet;
import edu.hitsz.skill.ActiveSkill;
import edu.hitsz.skill.ActiveSkill;
import android.graphics.Paint;



public abstract class Game extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    protected Context context;

    public interface GameOverListener {
        void onGameOver(int score, String userName);
    }

    protected GameOverListener listener;
    protected SurfaceHolder holder;
    protected Thread gameThread;
    protected boolean isRunning = false;

    private boolean gameOverTriggered = false;

    // 游戏数据
    protected HeroAircraft heroAircraft;
    protected List<AbstractEnemy> enemyAircrafts;
    protected List<BaseBullet> heroBullets;
    protected List<BaseBullet> enemyBullets;
    protected List<BaseItem> droppedItems;

    protected int score = 0;
    protected int time = 0;
    protected int lastBossScore = 0;
    protected boolean isBossExist = false;

    // 周期控制
    protected final int cycleDuration = 200;
    protected int cycleTime = 0;
    protected final int timeInterval = 20;

    // 难度参数
    protected int enemyMaxNumber = 5;
    protected int bossInterval = 1000;
    protected double eliteEnemyRate = 0.2;
    protected double superEliteRate = 0.1;
    protected int bossHp = 600;
    protected int superEliteHp = 50;
    protected int eliteHp = 30;
    protected int mobHp = 30;
    protected int gameLevel = 0;

    protected Random random = new Random();

    // 背景
    protected android.graphics.Bitmap background;
    protected int backGroundTop = 0;
    protected int screenWidth, screenHeight;

    // 音乐管理
    protected PlaySoundManager playSoundManager;

    protected String userName;

    public Game(Context context, String musicMode, String userName, GameOverListener listener) {
        super(context);
        this.context = context;
        this.listener = listener;
        this.context = context;
        this.userName = userName;
        holder = getHolder();
        holder.addCallback(this);
        setFocusable(true);
        setZOrderOnTop(true);
        holder.setFormat(PixelFormat.TRANSLUCENT); // 支持透明背景

        screenWidth = GameConfig.getInstance().getScreenWidth();
        screenHeight = GameConfig.getInstance().getScreenHeight();

        // 初始化英雄机
        heroAircraft = HeroAircraft.getInstance();
        initHeroAircraft();

        enemyAircrafts = new LinkedList<>();
        heroBullets = new LinkedList<>();
        enemyBullets = new LinkedList<>();
        droppedItems = new LinkedList<>();

        playSoundManager = new PlaySoundManager(context, musicMode.equals("ON"));
    }

    // ---------- SurfaceHolder.Callback ----------
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        isRunning = true;
        gameThread = new Thread(this);
        gameThread.start();
        // 启动背景音乐
        playSoundManager.playBgm();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // 可更新屏幕尺寸，但此处已通过GameConfig获取
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isRunning = false;
        try {
            gameThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // ---------- Runnable (游戏循环) ----------
    @Override
    public void run() {
        while (isRunning) {
            long startTime = System.currentTimeMillis();

            // 执行一帧逻辑
            updateGame();

            // 绘制
            drawGame();

            // 控制帧率
            long endTime = System.currentTimeMillis();
            long diff = endTime - startTime;
            if (diff < timeInterval) {
                try {
                    Thread.sleep(timeInterval - diff);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    protected void updateGame() {
        time += timeInterval;

        if (timeCountAndNewCycleJudge()) {
            // 产生新敌机
            if (enemyAircrafts.size() < enemyMaxNumber) {
                AbstractEnemy enemy = getEnemy();
                enemyAircrafts.add(enemy);
                heroAircraft.addObserver(enemy);
                checkForBossSpawn();
            }
            // 射击
            shootAction();

            for (BaseBullet bullet : heroBullets) {
                if (bullet instanceof LaserBullet) {
                    bullet.setLocation(
                            heroAircraft.getLocationX(),
                            heroAircraft.getLocationY()/2  // 激光从屏幕顶部开始
                    );
                }
            }
            // 难度提升
            increaseDifficulty();
        }

        // 技能持续时间更新
        checkSkillDuration();

        // 移动
        bulletsMoveAction();
        aircraftsMoveAction();
        itemsMoveAction();

        // 碰撞检测
        crashCheckAction();

        // 后处理（移除无效对象）
        postProcessAction();

        // 更新技能
        heroAircraft.updateSkills(timeInterval);

        // 检查游戏结束
        if (heroAircraft.getHp() <= 0) {
            gameOver();
        }
    }

    protected void drawGame() {
        Canvas canvas = holder.lockCanvas();
        if (canvas != null) {
            // 清屏，消除重影
            canvas.drawColor(Color.BLACK);

            if (background != null) {
                // 绘制下方图片
                canvas.drawBitmap(background, 0, backGroundTop, null);
                // 绘制上方图片（用于滚动衔接）
                canvas.drawBitmap(background, 0, backGroundTop - screenHeight, null);
            }
            backGroundTop += 1;
            if (backGroundTop >= screenHeight) {
                backGroundTop = 0;
            }

            // 绘制其他对象
            drawObjects(canvas, enemyBullets);
            drawObjects(canvas, droppedItems);
            drawObjects(canvas, heroBullets);
            drawObjects(canvas, enemyAircrafts);

            // 动态获取当前英雄机图片
            android.graphics.Bitmap heroImage = heroAircraft.getHeroImage();
            canvas.drawBitmap(heroImage,
                    heroAircraft.getLocationX() - heroImage.getWidth() / 2,
                    heroAircraft.getLocationY() - heroImage.getHeight() / 2,
                    null);

            if (heroAircraft.isInvincible()) {
                android.graphics.Bitmap shieldImage = ImageManager.SHIELD_IMAGE;
                if (shieldImage != null) {
                    // 护盾绘制在英雄机周围，半径×3
                    int shieldWidth = (heroImage.getWidth() + 40) * 3;
                    int shieldHeight = (heroImage.getHeight() + 40) * 3;
                    android.graphics.Bitmap scaledShield = android.graphics.Bitmap.createScaledBitmap(
                            shieldImage, shieldWidth, shieldHeight, true);
                    canvas.drawBitmap(scaledShield,
                            heroAircraft.getLocationX() - shieldWidth / 2,
                            heroAircraft.getLocationY() - shieldHeight / 2,
                            null);
                }
            }

            // ===== 绘制技能按钮 =====
            drawSkillButton(canvas);

            drawScoreAndLife(canvas);

            holder.unlockCanvasAndPost(canvas);
        }
    }

    protected void drawObjects(Canvas canvas, List<? extends AbstractFlyingObject> objects) {
        for (AbstractFlyingObject obj : objects) {
            android.graphics.Bitmap bmp = obj.getImage();
            if (bmp != null) {
                // 激光特殊绘制：从飞机位置向上重复绘制直至屏幕顶端
                if (obj instanceof LaserBullet) {
                    LaserBullet laser = (LaserBullet) obj;
                    int laserX = obj.getLocationX() - bmp.getWidth() / 2;
                    int currentY = obj.getLocationY(); // 从飞机位置开始

                    // 向上重复绘制激光图片，直到到达屏幕顶端
                    while (currentY > -bmp.getHeight()) {
                        canvas.drawBitmap(bmp, laserX, currentY - bmp.getHeight() / 2, null);
                        currentY -= bmp.getHeight();
                    }
                } else {
                    // 普通对象的绘制
                    canvas.drawBitmap(bmp,
                            obj.getLocationX() - bmp.getWidth() / 2,
                            obj.getLocationY() - bmp.getHeight() / 2,
                            null);
                }
            }
        }
    }

    protected void drawScoreAndLife(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setTextSize(40);
        canvas.drawText("SCORE: " + score, 10, 50, paint);
        canvas.drawText("LIFE: " + heroAircraft.getHp(), 10, 100, paint);

        // 显示道具效果时间
        float effect = heroAircraft.getEffectTimer() / 1000f;
        if (effect > 0) {
            paint.setColor(Color.YELLOW);
            canvas.drawText("道具效果: " + effect + "s", 10, 150, paint);
        }

        // 显示主动技能信息
        ActiveSkill activeSkill = heroAircraft.getActiveSkill();
        if (activeSkill != null) {
            paint.setTextSize(35);
            paint.setColor(Color.CYAN);
            String skillName = activeSkill.getName();
            int energy = activeSkill.getEnergy();
            int maxEnergy = activeSkill.getMaxEnergy();

            if (activeSkill.isActive()) {
                paint.setColor(Color.GREEN);
                canvas.drawText(skillName + " 激活中!", 10, 200, paint);
            } else if (activeSkill.canUse()) {
                paint.setColor(Color.GREEN);
                canvas.drawText(skillName + " [双击释放]", 10, 200, paint);
            } else {
                paint.setColor(Color.GRAY);
                int percent = (energy * 100) / maxEnergy;
                canvas.drawText(skillName + " 充能: " + percent + "%", 10, 200, paint);
            }
        }
    }

    // ---------- 触摸事件 ----------
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();

        if (event.getAction() == MotionEvent.ACTION_DOWN ||
                event.getAction() == MotionEvent.ACTION_MOVE) {

            // 定义技能按钮区域（右下角 150x150 像素）
            int skillAreaSize = 150;
            int skillAreaLeft = screenWidth - skillAreaSize - 20;
            int skillAreaTop = screenHeight - skillAreaSize - 100;
            int skillAreaRight = screenWidth - 20;
            int skillAreaBottom = screenHeight - 100;

            // 检查是否点击了技能区域
            if (x >= skillAreaLeft && x <= skillAreaRight &&
                    y >= skillAreaTop && y <= skillAreaBottom) {

                // 触发主动技能（仅在按下时触发，移动时不重复触发）
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    triggerActiveSkill();
                }
            } else {
                // 其他区域：移动英雄机
                if (x >= 0 && x <= screenWidth && y >= 0 && y <= screenHeight) {
                    heroAircraft.setLocation(x, y);
                }
            }
        }
        return true;
    }

    /**
     * 触发主动技能
     */
    private void triggerActiveSkill() {
        ActiveSkill skill = heroAircraft.getActiveSkill();
        if (skill != null && skill.canUse()) {
            // 使用技能
            java.util.List<BaseBullet> bullets = heroAircraft.useActiveSkill();
            if (!bullets.isEmpty()) {
                heroBullets.addAll(bullets);
                playSoundManager.playBulletHit();
            }
        }
    }


    // ---------- 游戏逻辑（保持原有方法，但需适配Android）----------
    protected void checkForBossSpawn() {
        int deltaScore = score - lastBossScore;
        if (deltaScore > bossInterval && !isBossExist) {
            lastBossScore = score;
            AbstractEnemy enemy;
            EnemyFactory factory;
            factory = new BossFactory();
            enemy = factory.createEnemy(bossHp);
            enemyAircrafts.add(enemy);
            isBossExist = true;
            playSoundManager.stopBgm();
            playSoundManager.playBgmBoss();
        } else if (isBossExist) {
            lastBossScore = score;
        }
    }

    protected void increaseDifficulty() {
        if (time > 600) {
            int difficultyRate = time / 600;
            // 每18s，增加敌机属性，上限20次
            if (difficultyRate % 30 == 0 && difficultyRate <= 600) {
                eliteEnemyRate += gameLevel * 0.01;
                superEliteRate += gameLevel * 0.01;
                mobHp += gameLevel;
                eliteHp += gameLevel * 2;
                superEliteHp += gameLevel * 3;
                System.out.println("-------敌机属性提升！-------\n" + "普通敌人：" + mobHp + "\n" + "精英敌人：" + eliteHp + "\n" + "超级敌人：" + superEliteHp + "\n");
            }
            // 每分钟，增加飞机数量，上限10
            if (difficultyRate % 100 == 0 && enemyMaxNumber <= 10) {
                enemyMaxNumber += gameLevel;
                System.out.println("-------敌机数量上限提升！-------\n" + "当前数量上限：" + enemyMaxNumber + "\n");
            }
            // 每30s，降低射速,上限10次
            if (difficultyRate % 50 == 0 && difficultyRate <= 500) {
                heroAircraft.addInterval(gameLevel * 100);
                System.out.println("-------英雄机射速降低！-------\n"+"当前射击间隔(小于600取600)：" + heroAircraft.getInterval() + "\n");
            }
        }
    }

    protected AbstractEnemy getEnemy() {
        double enemyType = random.nextDouble();
        if (enemyType < (1 - superEliteRate - eliteEnemyRate)) {
            return new MobEnemyFactory().createEnemy(mobHp);
        } else if (enemyType < (1 - superEliteRate)) {
            return new EliteEnemyFactory().createEnemy(eliteHp);
        } else {
            return new SuperEliteFactory().createEnemy(superEliteHp);
        }
    }

    protected boolean timeCountAndNewCycleJudge() {
        cycleTime += timeInterval;
        if (cycleTime >= cycleDuration) {
            cycleTime %= cycleDuration;
            return true;
        }
        return false;
    }

    protected void shootAction() {
        for (AbstractEnemy enemy : enemyAircrafts) {
            enemyBullets.addAll(enemy.executeStrategy());
        }
        heroBullets.addAll(heroAircraft.executeStrategy());
    }

    protected void bulletsMoveAction() {
        for (BaseBullet b : heroBullets) b.forward();
        for (BaseBullet b : enemyBullets) b.forward();
    }

    protected void aircraftsMoveAction() {
        for (AbstractEnemy e : enemyAircrafts) e.forward();
    }

    protected void itemsMoveAction() {
        for (BaseItem i : droppedItems) i.forward();
    }

    protected void checkSkillDuration() {
        if (heroAircraft.getEffectTimer() > 0) {
            heroAircraft.effectTimerUpdate(timeInterval);
        } else if (!heroAircraft.isReset) {
            heroAircraft.resetStrategy();
        }
    }

    protected void crashCheckAction() {
        // 敌机子弹攻击英雄
        for (BaseBullet bullet : enemyBullets) {
            if (bullet.notValid()) continue;
            if (heroAircraft.crash(bullet)) {
                playSoundManager.playBulletHit();
                heroAircraft.decreaseHp(bullet.getPower());
                bullet.vanish();
            }
        }

        // 英雄子弹攻击敌机
        for (BaseBullet bullet : heroBullets) {
            if (bullet.notValid()) continue;
            for (AbstractEnemy enemy : enemyAircrafts) {
                if (enemy.notValid()) continue;
                if (enemy.crash(bullet)) {
                    // ===== 穿透子弹特殊处理 =====
                    boolean isPenetrating = false;

                    // 检查是否为闪电子弹
                    if (bullet instanceof ArcLightning) {
                        isPenetrating = true;
                        // 闪电直接造成伤害,不消失
                        playSoundManager.playBulletHit();
                        enemy.decreaseHp(bullet.getPower());
                    }
                    // 检查是否为激光子弹
                    else if (bullet instanceof LaserBullet) {
                        LaserBullet laser = (LaserBullet) bullet;
                        // 激光需要检查伤害间隔
                        if (laser.canDamage()) {
                            playSoundManager.playBulletHit();
                            enemy.decreaseHp(bullet.getPower());
                        }
                        isPenetrating = true;
                    }
                    // 普通子弹
                    else {
                        playSoundManager.playBulletHit();
                        enemy.decreaseHp(bullet.getPower());
                        bullet.vanish();
                    }

                    // 检查敌机是否被击毁
                    if (enemy.notValid()) {
                        if (enemy instanceof BossEnemy) {
                            playSoundManager.stopBgmBoss();
                            playSoundManager.playBgm();
                            isBossExist = false;
                        }
                        droppedItems.addAll(enemy.spawnItems());
                        score += enemy.getScores();
                    }
                }

                // 英雄与敌机相撞
                if (enemy.crash(heroAircraft) || heroAircraft.crash(enemy)) {
                    if (enemy instanceof BossEnemy) {
                        playSoundManager.stopBgmBoss();
                        playSoundManager.playBgm();
                        isBossExist = false;
                    }
                    enemy.vanish();
                    heroAircraft.decreaseHp(Integer.MAX_VALUE);
                }
            }
        }

        // 道具收集
        for (BaseItem item : droppedItems) {
            if (item.notValid()) continue;
            if (heroAircraft.crash(item)) {
                if (item instanceof BombItem) {
                    playSoundManager.playBombExplosion();
                } else {
                    playSoundManager.playGetSupply();
                }
                item.activateEffect(heroAircraft);
                score += heroAircraft.getScores();
                item.vanish();
            }
        }
    }


    protected void initHeroAircraft() {
        heroAircraft.init(screenWidth / 2,
                screenHeight - ImageManager.HERO_IMAGE.getHeight(),
                0, 0, 100); // 默认 100
    }

    protected void postProcessAction() {
        enemyBullets.removeIf(obj -> obj.notValid());
        heroBullets.removeIf(obj -> obj.notValid());
        enemyAircrafts.removeIf(obj -> obj.notValid());
        droppedItems.removeIf(obj -> obj.notValid());
        heroAircraft.removeInvalid();
    }

    /**
     * 绘制技能按钮
     */
    private void drawSkillButton(Canvas canvas) {
        ActiveSkill skill = heroAircraft.getActiveSkill();
        if (skill == null) return; // 没有技能则不绘制

        // 技能按钮尺寸和位置
        int buttonSize = 120;
        int margin = 20;
        int bottomOffset = 100; // 距离底部的偏移
        int buttonX = screenWidth - buttonSize - margin;
        int buttonY = screenHeight - buttonSize - bottomOffset;

        // 绘制半透明背景
        Paint bgPaint = new Paint();
        bgPaint.setAntiAlias(true);

        // 根据技能状态设置颜色
        if (skill.isActive()) {
            // 技能激活中：绿色
            bgPaint.setColor(0x8800FF00);
        } else if (skill.canUse()) {
            // 可以使用：蓝色
            bgPaint.setColor(0x8800BFFF);
        } else {
            // 冷却中：灰色半透明
            bgPaint.setColor(0x66666666);
        }

        // 绘制圆形背景
        canvas.drawCircle(buttonX + buttonSize/2, buttonY + buttonSize/2,
                buttonSize/2, bgPaint);

        // 绘制边框
        Paint borderPaint = new Paint();
        borderPaint.setAntiAlias(true);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(3);

        if (skill.canUse()) {
            borderPaint.setColor(0xFF00FFFF); // 青色边框
        } else {
            borderPaint.setColor(0xFF888888); // 灰色边框
        }

        canvas.drawCircle(buttonX + buttonSize/2, buttonY + buttonSize/2,
                buttonSize/2, borderPaint);

        // 绘制技能图标
        android.graphics.Bitmap skillImage = ImageManager.get(skill.getClass().getName());
        if (skillImage != null) {
            int iconSize = buttonSize - 20;
            android.graphics.Bitmap scaledIcon = android.graphics.Bitmap.createScaledBitmap(
                    skillImage, iconSize, iconSize, true);
            canvas.drawBitmap(scaledIcon,
                    buttonX + (buttonSize - iconSize) / 2,
                    buttonY + (buttonSize - iconSize) / 2,
                    null);
        }

        // 绘制技能状态文本
        Paint textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(28);
        textPaint.setFakeBoldText(true);
        textPaint.setTextAlign(Paint.Align.CENTER);

        String statusText;
        if (skill.isActive()) {
            textPaint.setColor(0xFF00FF00); // 绿色
            statusText = "激活中";
        } else if (skill.canUse()) {
            textPaint.setColor(0xFF00FFFF); // 青色
            statusText = "点击释放";
        } else {
            // 显示充能或冷却
            int energy = skill.getEnergy();
            int maxEnergy = skill.getMaxEnergy();
            int percent = (energy * 100) / maxEnergy;

            long remainingCooldown = skill.getRemainingCooldown();
            if (remainingCooldown > 0) {
                textPaint.setColor(0xFFFF6666); // 红色
                statusText = (remainingCooldown / 1000) + "s";
            } else {
                textPaint.setColor(0xFFFFFFFF); // 白色
                statusText = percent + "%";
            }
        }

        // 文本绘制在按钮下方
        canvas.drawText(statusText, buttonX + buttonSize/2,
                buttonY + buttonSize + 25, textPaint);

        // 绘制技能名称（更上方）
        textPaint.setTextSize(22);
        textPaint.setColor(0xFFFFFF00); // 黄色
        canvas.drawText(skill.getName(), buttonX + buttonSize/2,
                buttonY - 15, textPaint);
    }


    protected void gameOver() {
        if (gameOverTriggered) return; // 防止重复调用
        gameOverTriggered = true;

        playSoundManager.stopBgm();
        playSoundManager.playGameOver();
        try { Thread.sleep(800); } catch (InterruptedException e) { e.printStackTrace(); }
        playSoundManager.shutdown();
        if (listener != null) {
            listener.onGameOver(score, userName);
        }
    }

    public void setBackground(android.graphics.Bitmap bg) {
        if (bg != null) {
            // 将背景图片缩放到屏幕尺寸，消除黑边
            this.background = android.graphics.Bitmap.createScaledBitmap(bg, screenWidth, screenHeight, true);
        }
    }

    public void resumeGame() {
        if (playSoundManager != null) {
            // 根据当前是否有Boss决定播放哪种背景音乐
            if (isBossExist) {
                playSoundManager.playBgmBoss();
            } else {
                playSoundManager.playBgm();
            }
        }
    }

    public void pauseGame() {
        if (playSoundManager != null) {
            playSoundManager.stopBgm();
            playSoundManager.stopBgmBoss();
        }
    }
}