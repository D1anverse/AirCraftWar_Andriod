package com.game.myserver.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 数据库管理器 - 单例模式
 * 使用 SQLite 存储用户、好友、玩家数据
 *
 * 数据库文件自动创建在服务器运行目录下: game_server.db
 *
 * v2.0: 增加连接健康检查 + 自动重连机制
 */
public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:game_server.db";
    private static volatile DatabaseManager instance;

    private Connection connection;
    private final Object connectionLock = new Object(); // 连接操作的锁对象

    // ===== 表名 =====
    public static final String TABLE_USERS = "users";
    public static final String TABLE_FRIENDS = "friends";

    private DatabaseManager() {
        initDatabase();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * 初始化数据库连接并建表（如果不存在）
     */
    private void initDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            connect();
            System.out.println("[DB] 数据库连接成功: " + DB_URL);

            createTables();
        } catch (Exception e) {
            System.err.println("[DB] 数据库初始化失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("数据库启动失败", e);
        }
    }

    /**
     * 建立/重新建立连接
     */
    private void connect() throws SQLException, ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection(DB_URL);

        // ★ 启用 WAL 模式，提升并发性能
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");       // 并发读写性能大幅提升
            stmt.execute("PRAGMA busy_timeout=5000");      // 锁冲突时等待5秒而非立即失败
        }
    }

    /**
     * 获取数据库连接（带健康检查和自动重连）
     */
    public Connection getConnection() {
        synchronized (connectionLock) {
            if (!isConnectionValid()) {
                System.out.println("[DB] 连接无效，尝试重新连接...");
                try {
                    // 尝试关闭旧连接
                    if (connection != null) {
                        try { connection.close(); } catch (SQLException ignored) {}
                    }
                    connect();
                    System.out.println("[DB] 重连成功");
                } catch (Exception e) {
                    System.err.println("[DB] 重连失败: " + e.getMessage());
                    throw new RuntimeException("无法连接到数据库", e);
                }
            }
            return connection;
        }
    }

    /**
     * 检查连接是否有效
     */
    public boolean isConnectionValid() {
        if (connection == null) return false;
        try {
            return !connection.isClosed() && connection.isValid(1);
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * 建表（包含索引创建）
     */
    private void createTables() throws SQLException {
        Statement stmt = connection.createStatement();

        // ========== 用户表 ==========
        String sqlUsers = "CREATE TABLE IF NOT EXISTS " + TABLE_USERS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "nickname TEXT NOT NULL UNIQUE, "
                + "email TEXT NOT NULL UNIQUE, "
                + "password TEXT NOT NULL, "
                + "coins INTEGER DEFAULT 0, "
                + "unlocked_pro INTEGER DEFAULT 0, "
                + "unlocked_promax INTEGER DEFAULT 0, "
                + "created_at DATETIME DEFAULT CURRENT_TIMESTAMP"
                + ")";
        stmt.execute(sqlUsers);

        // ========== 好友关系表 ==========
        String sqlFriends = "CREATE TABLE IF NOT EXISTS " + TABLE_FRIENDS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "user_id INTEGER NOT NULL, "
                + "friend_id INTEGER NOT NULL, "
                + "status TEXT DEFAULT 'pending', "
                + "created_at DATETIME DEFAULT CURRENT_TIMESTAMP"
                + ")";
        stmt.execute(sqlFriends);

        // ========== 索引优化 ==========
        // 好友表常用查询字段索引，加速好友列表查询、添加好友检查等操作
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_friends_user_id ON friends(user_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_friends_friend_id ON friends(friend_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_friends_status ON friends(status)");
        // 联合索引：加速"查询某人的所有已接受好友"场景
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_friends_user_status ON friends(user_id, status)");
        // 联合索引：加速双向关系检查
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_friends_pair ON friends(user_id, friend_id)");

        stmt.close();
        System.out.println("[DB] 数据表及索引检查完毕");
    }

    /**
     * 安全关闭资源
     * 支持变长参数，可一次性关闭多个资源（如 ResultSet, Statement, Connection 等）
     *
     * @param resources 需要关闭的资源数组
     */
    public static void closeQuietly(AutoCloseable... resources) {
        if (resources == null) return;
        for (AutoCloseable r : resources) {
            if (r != null) {
                try { r.close(); } catch (Exception ignored) {}
            }
        }
    }
}
