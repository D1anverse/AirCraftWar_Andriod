package edu.hitsz.application;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import edu.hitsz.R;
import edu.hitsz.application.ShopAdapter;
import edu.hitsz.dao.PlayerDataManager;
import edu.hitsz.entity.PlayerData;
import edu.hitsz.network.SocketClient;

/**
 * 商店页面
 *
 * 功能：
 *   1. 显示当前代币数量
 *   2. 展示可购买的战机（PRO: 5000, PROMAX: 15000）
 *   3. 购买时扣除本地代币 + 发送同步请求到服务器
 *   4. BASIC 默认免费不展示
 */
public class ShopActivity extends AppCompatActivity implements ShopAdapter.OnBuyClickListener {

    private TextView tvCoinsDisplay;
    private Button btnBack;
    private RecyclerView recyclerView;
    private ShopAdapter adapter;

    private PlayerDataManager playerDataManager;
    private UserSessionManager sessionManager;
    private PlayerData playerData;
    private volatile boolean coinsRefreshed = false;
    private volatile boolean purchaseSuccess = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop);

        playerDataManager = new PlayerDataManager(this);
        sessionManager = UserSessionManager.getInstance(this);

        initViews();
        loadPlayerData();
        setupRecyclerView();
        setupListeners();
    }

    private void initViews() {
        tvCoinsDisplay = findViewById(R.id.tv_coins_display);
        btnBack = findViewById(R.id.btn_back);
        recyclerView = findViewById(R.id.recycler_shop);
    }

    private void loadPlayerData() {
        int userId = sessionManager.getUserId();
        if (userId > 0) {
            playerData = playerDataManager.getOrCreatePlayerData(userId);
            tvCoinsDisplay.setText("💰 " + playerData.getCoins());
        }
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ShopAdapter(this,
                playerData.isUnlockedPro(),
                playerData.isUnlockedPromax());
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
    }

    /**
     * 购买回调
     */
    @Override
    public void onBuy(final String aircraftType, final int price) {
        // 先检查本地代币够不够
        if (playerData.getCoins() < price) {
            Toast.makeText(this, "代币不足！需要 " + price + "，当前 " + playerData.getCoins(),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // 弹出确认对话框
        String aircraftName = "PRO".equals(aircraftType) ? "进阶型" : "旗舰型";
        new AlertDialog.Builder(this)
                .setTitle("确认购买")
                .setMessage("花费 " + price + " 代币解锁 " + aircraftName + " 战机？")
                .setPositiveButton("购买", (dialog, which) -> executePurchase(aircraftType, price))
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 执行购买操作
     * 1. 同步到服务器
     * 2. 服务器处理后返回完整数据
     * 3. 更新本地数据库
     */
    private void executePurchase(String aircraftType, int price) {
        int userId = sessionManager.getUserId();

        // 先检查本地代币是否足够
        if (playerData.getCoins() < price) {
            Toast.makeText(this, "代币不足！", Toast.LENGTH_SHORT).show();
            return;
        }

        SocketClient client = SocketClient.getInstance();
        if (!client.connected()) {
            Toast.makeText(this, "未连接到服务器", Toast.LENGTH_SHORT).show();
            return;
        }

        // 发送购买请求
        client.sendMessage("BUY_AIRCRAFT", userId + "," + aircraftType);

        // 禁用购买按钮防止重复点击
        btnBack.setEnabled(false);

        // 监听服务器返回
        client.setQueryCallback(new SocketClient.OnMessageReceivedListener() {
            @Override
            public void onMessageReceived(String type, String body) {
                if ("BUY_OK".equals(type)) {
                    // 服务器返回格式：newCoins,pro,promax
                    purchaseSuccess = true;
                    String[] parts = body.split(",");
                    if (parts.length >= 3) {
                        PlayerData pd = playerDataManager.getOrCreatePlayerData(userId);
                        pd.setCoins(Integer.parseInt(parts[0]));
                        pd.setUnlockedPro("1".equals(parts[1]));
                        pd.setUnlockedPromax("1".equals(parts[2]));
                        playerDataManager.insertOrUpdate(pd);
                        playerData = pd;

                        runOnUiThread(() -> {
                            String aircraftName = "PRO".equals(aircraftType) ? "进阶型" : "旗舰型";
                            Toast.makeText(ShopActivity.this, "购买成功！" + aircraftName + " 已解锁", Toast.LENGTH_SHORT).show();
                            tvCoinsDisplay.setText("💰 " + pd.getCoins());
                            adapter.updateUnlockStatus(null);
                            btnBack.setEnabled(true);
                        });
                    }
                    client.clearQueryCallback();
                } else if ("BUY_FAIL".equals(type)) {
                    runOnUiThread(() -> {
                        Toast.makeText(ShopActivity.this, "购买失败：" + body, Toast.LENGTH_SHORT).show();
                        // 回滚：重新加载本地数据
                        playerData = playerDataManager.getOrCreatePlayerData(userId);
                        tvCoinsDisplay.setText("💰 " + playerData.getCoins());
                        adapter.updateUnlockStatus(null);
                        btnBack.setEnabled(true);
                    });
                    client.clearQueryCallback();
                }
            }

            @Override
            public void onDisconnected() {
                client.clearQueryCallback();
                runOnUiThread(() -> {
                    Toast.makeText(ShopActivity.this, "服务器断开连接", Toast.LENGTH_SHORT).show();
                    btnBack.setEnabled(true);
                });
            }
        });

        // 2秒超时
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            client.clearQueryCallback();
            runOnUiThread(() -> {
                // 只有购买未成功时才显示超时提示，避免与购买成功 toast 冲突
                if (!purchaseSuccess) {
                    Toast.makeText(ShopActivity.this, "请求超时", Toast.LENGTH_SHORT).show();
                    playerData = playerDataManager.getOrCreatePlayerData(userId);
                    tvCoinsDisplay.setText("💰 " + playerData.getCoins());
                    adapter.updateUnlockStatus(null);
                }
                btnBack.setEnabled(true);
            });
        }, 2000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshCoins();
    }

    /**
     * 从服务器在线拉取代币，确保显示最新数据
     * 同时更新本地 SQLite，保证一致性
     * 若服务器 2 秒内未响应则回退到本地 SQLite
     */
    private void refreshCoins() {
        coinsRefreshed = false;
        if (!sessionManager.isLoggedIn() || sessionManager.getUserId() <= 0) {
            return;
        }

        SocketClient client = SocketClient.getInstance();
        int userId = sessionManager.getUserId();

        if (client.connected()) {
            // 向服务器请求最新代币（用 setQueryCallback 不覆盖主 listener）
            client.setQueryCallback(new SocketClient.OnMessageReceivedListener() {
                @Override
                public void onMessageReceived(String type, String body) {
                    if ("COINS_INFO".equals(type) && !coinsRefreshed) {
                        coinsRefreshed = true;
                        client.clearQueryCallback();
                        // 服务器返回格式：userId,coins,proUnlocked,promaxUnlocked
                        String[] parts = body.split(",");
                        int coinsForUi = 0;
                        if (parts.length >= 2) {
                            coinsForUi = Integer.parseInt(parts[1]);
                            // 更新本地 SQLite
                            PlayerData pd = playerDataManager.getOrCreatePlayerData(userId);
                            pd.setCoins(coinsForUi);
                            if (parts.length >= 3) {
                                pd.setUnlockedPro("1".equals(parts[2]));
                            }
                            if (parts.length >= 4) {
                                pd.setUnlockedPromax("1".equals(parts[3]));
                            }
                            playerDataManager.insertOrUpdate(pd);
                            playerData = pd;
                        }
                        final int finalCoins = coinsForUi;
                        runOnUiThread(() -> {
                            tvCoinsDisplay.setText("💰 " + finalCoins);
                            adapter.updateUnlockStatus(null);
                        });
                    }
                }

                @Override
                public void onDisconnected() {
                    client.clearQueryCallback();
                    runOnUiThread(() -> {
                        if (!coinsRefreshed) {
                            playerData = playerDataManager.getOrCreatePlayerData(userId);
                            tvCoinsDisplay.setText("💰 " + playerData.getCoins());
                            coinsRefreshed = true;
                        }
                    });
                }
            });

            client.sendQueryCoins(userId);

            // 超时回退：服务器 2 秒内未响应则读本地 SQLite
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (!coinsRefreshed) {
                    coinsRefreshed = true;
                    client.clearQueryCallback();
                    playerData = playerDataManager.getOrCreatePlayerData(userId);
                    tvCoinsDisplay.setText("💰 " + playerData.getCoins());
                }
            }, 2000);
        } else {
            // 离线模式：直接读本地 SQLite
            playerData = playerDataManager.getOrCreatePlayerData(userId);
            tvCoinsDisplay.setText("💰 " + playerData.getCoins());
            coinsRefreshed = true;
        }
    }
}

