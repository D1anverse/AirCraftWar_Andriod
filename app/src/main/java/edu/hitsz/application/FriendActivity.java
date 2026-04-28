package edu.hitsz.application;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import edu.hitsz.R;
import edu.hitsz.application.FriendAdapter;
import edu.hitsz.network.SocketClient;

/**
 * 好友页面
 *
 * 功能：
 *   1. 搜索用户（发送 SEARCH_USER|keyword 到服务器）
 *   2. 显示好友列表（从服务器拉取 FRIEND_LIST|userId）
 *   3. 添加好友（发送 ADD_FRIEND|myUserId,targetUserId）
 *   4. 邀请好友对战（发送 INVITE_FRIEND|myUserId,friendId）
 *   5. 处理好友请求通知（FRIEND_REQUEST）
 */
public class FriendActivity extends AppCompatActivity implements FriendAdapter.OnFriendActionListener {

    private EditText etSearch;
    private Button btnSearch, btnBack;
    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private FriendAdapter adapter;
    private List<Map<String, String>> friendList = new ArrayList<>();

    private UserSessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend);

        sessionManager = UserSessionManager.getInstance(this);

        initViews();
        setupRecyclerView();
        setupListeners();
        setupGlobalMessageListener();

        // 自动加载好友列表
        loadFriendList();
    }

    private void initViews() {
        etSearch = findViewById(R.id.et_search);
        btnSearch = findViewById(R.id.btn_search);
        btnBack = findViewById(R.id.btn_back);
        recyclerView = findViewById(R.id.recycler_friends);
        tvEmpty = findViewById(R.id.tv_empty);
    }

    private void setupRecyclerView() {
        adapter = new FriendAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnSearch.setOnClickListener(v -> {
            String keyword = etSearch.getText().toString().trim();
            if (TextUtils.isEmpty(keyword)) {
                Toast.makeText(this, "请输入搜索关键词", Toast.LENGTH_SHORT).show();
                return;
            }
            searchUser(keyword);
        });
    }

    /**
     * 全局消息监听器（统一处理所有服务器消息）
     * 修复：不再每次操作覆盖监听器
     */
    private void setupGlobalMessageListener() {
        SocketClient client = SocketClient.getInstance();
        client.setMessageListener(new SocketClient.OnMessageReceivedListener() {
            @Override
            public void onMessageReceived(String type, String body) {
                runOnUiThread(() -> {
                    switch (type) {
                        case "SEARCH_RESULT":
                            handleSearchResult(body);
                            break;
                        case "FRIEND_LIST":
                            handleFriendList(body);
                            break;
                        case "ADD_FRIEND_OK":
                            Toast.makeText(FriendActivity.this, "好友请求已发送", Toast.LENGTH_SHORT).show();
                            loadFriendList();
                            break;
                        case "ADD_FRIEND_FAIL":
                            Toast.makeText(FriendActivity.this, "添加失败：" + body, Toast.LENGTH_SHORT).show();
                            break;
                        case "FRIEND_REQUEST":
                            // 收到好友申请通知
                            showFriendRequestDialog(body);
                            break;
                        case "FRIEND_ACCEPTED":
                            Toast.makeText(FriendActivity.this, body + " 同意了您的好友申请", Toast.LENGTH_SHORT).show();
                            loadFriendList();
                            break;
                        case "INVITE_REQUEST":
                            // 收到对战邀请通知
                            showInviteRequestDialog(body);
                            break;
                        case "INVITE_OK":
                            Toast.makeText(FriendActivity.this, "邀请已发送", Toast.LENGTH_SHORT).show();
                            break;
                        case "INVITE_REJECTED":
                            Toast.makeText(FriendActivity.this, body + " 拒绝了您的对战邀请", Toast.LENGTH_SHORT).show();
                            break;
                        case "INVITE_ACCEPTED":
                            // 对方接受了邀请，跳转到联机对战房间
                            Toast.makeText(FriendActivity.this, body + " 接受了您的对战邀请，正在进入房间...", Toast.LENGTH_SHORT).show();
                            goToOnlineLobby();
                            break;
                        case "INVITE_CONFIRMED":
                            // 服务器确认邀请已接受，进入房间
                            Toast.makeText(FriendActivity.this, "邀请已确认，正在进入房间...", Toast.LENGTH_SHORT).show();
                            goToOnlineLobby();
                            break;
                    }
                });
            }

            @Override
            public void onDisconnected() {
                runOnUiThread(() -> Toast.makeText(FriendActivity.this, "连接已断开", Toast.LENGTH_SHORT).show());
            }
        });
    }

    /**
     * 跳转到联机对战房间
     */
    private void goToOnlineLobby() {
        Intent intent = new Intent(FriendActivity.this, edu.hitsz.network.OnlineLobbyActivity.class);
        intent.putExtra("username", sessionManager.getNickname());
        intent.putExtra("musicMode", getIntent().getStringExtra("musicMode"));
        startActivity(intent);
        finish();
    }

    /**
     * 搜索用户
     * 发送协议：SEARCH_USER|keyword
     * 期望返回：SEARCH_RESULT|friendId,friendName,status
     */
    private void searchUser(String keyword) {
        SocketClient client = SocketClient.getInstance();
        if (!client.connected()) {
            Toast.makeText(this, "未连接服务器", Toast.LENGTH_SHORT).show();
            return;
        }
        client.sendMessage("SEARCH_USER", keyword);
        Toast.makeText(this, "搜索中...", Toast.LENGTH_SHORT).show();
    }

    /**
     * 加载好友列表
     * 发送协议：GET_FRIENDS|userId
     */
    private void loadFriendList() {
        SocketClient client = SocketClient.getInstance();
        if (!client.connected()) {
            showEmptyView(true);
            return;
        }
        int userId = sessionManager.getUserId();
        adapter.setSearchResultMode(false);
        friendList.clear();
        adapter.setData(friendList);
        client.sendMessage("GET_FRIENDS", String.valueOf(userId));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 每次回到好友页面时刷新好友列表
        loadFriendList();
    }

    /**
     * 处理搜索结果
     * 格式示例：friendId,friendName,status （单条结果）
     */
    private void handleSearchResult(String body) {
        String[] parts = body.split(",");
        if (parts.length >= 2) {
            Map<String, String> result = new HashMap<>();
            result.put("friendId", parts[0]);
            result.put("friendName", parts[1]);
            result.put("status", parts.length >= 3 ? parts[2] : "offline");

            friendList.clear();
            friendList.add(result);
            adapter.setData(friendList);
            adapter.setSearchResultMode(true);
            showEmptyView(false);
        } else {
            Toast.makeText(this, "未找到用户", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 处理好友列表返回
     * 格式示例：每条好友用空格分隔，字段用逗号分隔
     */
    private void handleFriendList(String body) {
        friendList.clear();
        adapter.setSearchResultMode(false);

        if (body == null || body.trim().isEmpty() || body.trim().equals("NONE")) {
            adapter.setData(friendList);
            showEmptyView(true);
            return;
        }

        // 按空格分割，处理可能存在的多个好友
        String[] friends = body.trim().split("\\s+");
        for (String friend : friends) {
            String trimmed = friend.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String[] parts = trimmed.split(",");
            if (parts.length >= 2) {
                try {
                    Map<String, String> f = new HashMap<>();
                    f.put("friendId", parts[0].trim());
                    f.put("friendName", parts[1].trim());
                    f.put("status", parts.length >= 3 ? parts[2].trim() : "offline");
                    friendList.add(f);
                } catch (Exception ignored) {
                }
            }
        }

        adapter.setData(friendList);
        showEmptyView(friendList.isEmpty());
    }

    /**
     * 显示好友申请对话框
     * 格式：fromId,fromNickname
     */
    private void showFriendRequestDialog(String body) {
        String[] parts = body.split(",");
        if (parts.length < 2) return;
        int fromId = Integer.parseInt(parts[0]);
        String fromNickname = parts[1];

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("好友申请")
                .setMessage(fromNickname + " 想添加您为好友")
                .setPositiveButton("同意", (d, w) -> acceptFriend(fromId))
                .setNegativeButton("拒绝", (d, w) -> rejectFriend(fromId))
                .setNeutralButton("忽略", null)
                .show();
    }

    /**
     * 显示对战邀请对话框
     * 格式：fromId,fromNickname
     */
    private void showInviteRequestDialog(String body) {
        String[] parts = body.split(",");
        if (parts.length < 2) return;
        int fromId = Integer.parseInt(parts[0]);
        String fromNickname = parts[1];

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("对战邀请")
                .setMessage(fromNickname + " 邀请您进行对战")
                .setPositiveButton("接受", (d, w) -> acceptInvite(fromId))
                .setNegativeButton("拒绝", (d, w) -> rejectInvite(fromId))
                .show();
    }

    /**
     * 添加好友
     * 协议：ADD_FRIEND|myUserId,targetUserId
     */
    @Override
    public void onAddFriend(String friendName, int friendId) {
        SocketClient.getInstance().sendMessage(
                "ADD_FRIEND",
                sessionManager.getUserId() + "," + friendId
        );
        Toast.makeText(this, "已向 " + friendName + " 发送好友请求", Toast.LENGTH_SHORT).show();
    }

    /**
     * 邀请好友对战（适配器回调）
     * 协议：INVITE_FRIEND|myUserId,friendId
     */
    @Override
    public void onInvite(String friendName, int friendId) {
        SocketClient client = SocketClient.getInstance();
        if (!client.connected()) {
            Toast.makeText(this, "未连接服务器，无法邀请", Toast.LENGTH_SHORT).show();
            return;
        }
        client.sendMessage("INVITE_FRIEND",
                sessionManager.getUserId() + "," + friendId);
        Toast.makeText(this, "已向 " + friendName + " 发出对战邀请", Toast.LENGTH_SHORT).show();
    }

    /**
     * 同意好友申请
     */
    private void acceptFriend(int fromId) {
        SocketClient.getInstance().sendMessage("ACCEPT_FRIEND",
                sessionManager.getUserId() + "," + fromId);
        Toast.makeText(this, "已同意好友申请", Toast.LENGTH_SHORT).show();
    }

    /**
     * 拒绝好友申请
     */
    private void rejectFriend(int fromId) {
        SocketClient.getInstance().sendMessage("REJECT_FRIEND",
                sessionManager.getUserId() + "," + fromId);
    }

    /**
     * 接受对战邀请
     */
    private void acceptInvite(int fromId) {
        SocketClient.getInstance().sendMessage("ACCEPT_INVITE",
                sessionManager.getUserId() + "," + fromId);
    }

    /**
     * 拒绝对战邀请
     */
    private void rejectInvite(int fromId) {
        SocketClient.getInstance().sendMessage("REJECT_INVITE",
                sessionManager.getUserId() + "," + fromId);
    }

    private void showEmptyView(boolean isEmpty) {
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        tvEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }
}

