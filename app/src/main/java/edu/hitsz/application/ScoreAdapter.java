package edu.hitsz.application;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import edu.hitsz.dao.Score;
import edu.hitsz.dao.ScoreDAOImpl;
import edu.hitsz.R;

public class ScoreAdapter extends RecyclerView.Adapter<ScoreAdapter.ViewHolder> {
    private Context context;
    private List<Score> scores;
    private ScoreDAOImpl scoreDAO;

    public ScoreAdapter(Context context, List<Score> scores, ScoreDAOImpl scoreDAO) {
        this.context = context;
        this.scores = scores;
        this.scoreDAO = scoreDAO;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_score, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Score score = scores.get(position);
        holder.tvRank.setText(String.valueOf(position + 1));
        holder.tvName.setText(score.getUsername());
        holder.tvScore.setText(String.valueOf(score.getScore()));
        holder.tvTime.setText(score.getTime());

        holder.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("删除记录")
                    .setMessage("确定删除这条记录吗？")
                    .setPositiveButton("确定", (dialog, which) -> {
                        scoreDAO.deleteScore(score.getId());
                        if (context instanceof ScoreActivity) {
                            ((ScoreActivity) context).refreshData();
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() { return scores.size(); }

    public void updateData(List<Score> newScores) {
        this.scores = newScores;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRank, tvName, tvScore, tvTime;
        Button btnDelete;
        ViewHolder(View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tv_rank);
            tvName = itemView.findViewById(R.id.tv_name);
            tvScore = itemView.findViewById(R.id.tv_score);
            tvTime = itemView.findViewById(R.id.tv_time);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
