package edu.hitsz.application;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import edu.hitsz.R;

/**
 * 好友列表适配器
 *
 * 好友数据结构（从服务器获取后解析为 Map）：
 *   friendName - 好友昵称
 *   friendId   - 好友ID
 *   status     - 在线状态
 */
public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.FriendViewHolder> {

    private List<Map<String, String>> friendList = new ArrayList<>();
    private OnFriendActionListener listener;
    private boolean isSearchResultMode = false;

    public interface OnFriendActionListener {
        void onInvite(String friendName, int friendId);
        void onAddFriend(String friendName, int friendId);
    }

    public FriendAdapter(OnFriendActionListener listener) {
        this.listener = listener;
    }

    public void setData(List<Map<String, String>> data) {
        this.friendList = data;
        notifyDataSetChanged();
    }

    public void setSearchResultMode(boolean isSearchResult) {
        this.isSearchResultMode = isSearchResult;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend, parent, false);
        return new FriendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        Map<String, String> friend = friendList.get(position);

        holder.tvName.setText(friend.getOrDefault("friendName", "未知"));
        String status = friend.getOrDefault("status", "offline");
        holder.tvStatus.setText("online".equals(status) ? "在线" : "离线");
        holder.tvStatus.setTextColor("online".equals(status) ? 0xFF4CAF50 : 0xFF888888);

        int friendId = Integer.parseInt(friend.getOrDefault("friendId", "-1"));
        String friendName = friend.getOrDefault("friendName", "");

        // 根据模式显示不同按钮
        holder.btnInvite.setVisibility(isSearchResultMode ? View.GONE : View.VISIBLE);
        holder.btnAddFriend.setVisibility(isSearchResultMode ? View.VISIBLE : View.GONE);

        holder.btnInvite.setOnClickListener(v -> {
            if (listener != null && friendId > 0) {
                listener.onInvite(friendName, friendId);
            }
        });

        holder.btnAddFriend.setOnClickListener(v -> {
            if (listener != null && friendId > 0 && listener instanceof OnFriendActionListener) {
                ((OnFriendActionListener) listener).onAddFriend(friendName, friendId);
            }
        });
    }

    @Override
    public int getItemCount() {
        return friendList.size();
    }

    static class FriendViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvStatus;
        Button btnInvite, btnAddFriend;

        FriendViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_friend_name);
            tvStatus = itemView.findViewById(R.id.tv_friend_status);
            btnInvite = itemView.findViewById(R.id.btn_invite);
            btnAddFriend = itemView.findViewById(R.id.btn_add_friend);
        }
    }
}

