package edu.hitsz.application;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import edu.hitsz.R;

/**
 * 商店商品适配器
 * 展示可供购买的战机
 */
public class ShopAdapter extends RecyclerView.Adapter<ShopAdapter.ShopViewHolder> {

    private ShopItem[] items;
    private OnBuyClickListener listener;
    private boolean proUnlocked;
    private boolean promaxUnlocked;

    public interface OnBuyClickListener {
        void onBuy(String aircraftType, int price);
    }

    /**
     * 构造商品列表
     * BASIC 默认解锁不显示，只展示 PRO 和 PROMAX
     */
    public ShopAdapter(OnBuyClickListener listener, boolean proUnlocked, boolean promaxUnlocked) {
        this.listener = listener;
        this.proUnlocked = proUnlocked;
        this.promaxUnlocked = promaxUnlocked;

        items = new ShopItem[]{
                new ShopItem("PRO", "进阶型战机", "大范围子弹 + 雷霆技能", 5000, proUnlocked),
                new ShopItem("PROMAX", "旗舰型战机", "双翼射击 + 护盾技能", 15000, promaxUnlocked)
        };
    }

    @NonNull
    @Override
    public ShopViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shop_aircraft, parent, false);
        return new ShopViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShopViewHolder holder, int position) {
        ShopItem item = items[position];

        holder.tvName.setText(item.name);
        holder.tvDesc.setText(item.desc);
        holder.tvPrice.setText("💰 " + item.price);

        // 不同机型给不同颜色
        if ("PRO".equals(item.type)) {
            holder.viewIcon.setBackgroundColor(0xFFE67E22);
        } else {
            holder.viewIcon.setBackgroundColor(0xFF9B59B6);
        }

        if (item.isUnlocked) {
            // 已解锁
            holder.btnBuy.setText("✓ 已拥有");
            holder.btnBuy.setEnabled(false);
            holder.btnBuy.setBackgroundTintList(android.content.res.ColorStateList
                    .valueOf(0xFF666666));
            holder.tvPrice.setText("已解锁");
            holder.tvPrice.setTextColor(0xFF4CAF50);
        } else {
            // 未解锁
            holder.btnBuy.setText("购买");
            holder.btnBuy.setEnabled(true);
            holder.btnBuy.setBackgroundTintList(android.content.res.ColorStateList
                    .valueOf(0xFF27AE60));

            holder.btnBuy.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onBuy(item.type, item.price);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.length;
    }

    /** 更新解锁状态（购买成功后调用） */
    public void updateUnlockStatus(String aircraftType) {
        if ("PRO".equals(aircraftType)) {
            proUnlocked = true;
            items[0].isUnlocked = true;
        } else if ("PROMAX".equals(aircraftType)) {
            promaxUnlocked = true;
            items[1].isUnlocked = true;
        }
        notifyDataSetChanged();
    }

    static class ShopViewHolder extends RecyclerView.ViewHolder {
        View viewIcon;
        TextView tvName, tvDesc, tvPrice;
        Button btnBuy;

        ShopViewHolder(View itemView) {
            super(itemView);
            viewIcon = itemView.findViewById(R.id.view_icon);
            tvName = itemView.findViewById(R.id.tv_aircraft_name);
            tvDesc = itemView.findViewById(R.id.tv_aircraft_desc);
            tvPrice = itemView.findViewById(R.id.tv_price);
            btnBuy = itemView.findViewById(R.id.btn_buy);
        }
    }

    /** 商品数据模型 */
    static class ShopItem {
        String type;         // PRO / PROMAX
        String name;
        String desc;
        int price;
        boolean isUnlocked;

        ShopItem(String type, String name, String desc, int price, boolean isUnlocked) {
            this.type = type;
            this.name = name;
            this.desc = desc;
            this.price = price;
            this.isUnlocked = isUnlocked;
        }
    }
}

