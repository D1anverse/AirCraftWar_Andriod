package edu.hitsz.application;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 用户会话管理器（单例模式）
 * 使用 SharedPreferences 管理登录状态
 *
 * 存储内容：
 *   - 是否已登录
 *   - 用户ID（服务器分配）
 *   - 用户昵称
 *   - 邮箱
 */
public class UserSessionManager {

    private static final String PREF_NAME = "UserSession";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_NICKNAME = "nickname";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_SERVER_COINS = "server_coins";
    private static final String KEY_PENDING_COINS = "pending_coins"; // 待同步代币

    private static volatile UserSessionManager instance;
    private final SharedPreferences prefs;

    private UserSessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized UserSessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new UserSessionManager(context.getApplicationContext());
        }
        return instance;
    }

    /** 保存登录信息 */
    public void saveLoginInfo(int userId, String nickname, String email) {
        prefs.edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putInt(KEY_USER_ID, userId)
                .putString(KEY_NICKNAME, nickname)
                .putString(KEY_EMAIL, email)
                .apply();
    }

    /** 清除登录信息（退出登录） */
    public void clearLoginInfo() {
        prefs.edit().clear().apply();
    }

    /** 是否已登录 */
    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    /** 获取用户ID */
    public int getUserId() {
        return prefs.getInt(KEY_USER_ID, -1);
    }

    /** 获取用户昵称 */
    public String getNickname() {
        return prefs.getString(KEY_NICKNAME, "");
    }

    /** 获取邮箱 */
    public String getEmail() {
        return prefs.getString(KEY_EMAIL, "");
    }

    /** 保存服务器同步的代币数 */
    public void saveServerCoins(int coins) {
        prefs.edit().putInt(KEY_SERVER_COINS, coins).apply();
    }

    /** 获取缓存的代币数 */
    public int getCachedCoins() {
        return prefs.getInt(KEY_SERVER_COINS, 0);
    }

    /** 添加待同步的代币（单机模式缓存） */
    public void addPendingCoins(int coins) {
        int pending = prefs.getInt(KEY_PENDING_COINS, 0);
        prefs.edit().putInt(KEY_PENDING_COINS, pending + coins).apply();
    }

    /** 获取待同步的代币数 */
    public int getPendingCoins() {
        return prefs.getInt(KEY_PENDING_COINS, 0);
    }

    /** 清除待同步代币（同步成功后调用） */
    public void clearPendingCoins() {
        prefs.edit().putInt(KEY_PENDING_COINS, 0).apply();
    }
}
