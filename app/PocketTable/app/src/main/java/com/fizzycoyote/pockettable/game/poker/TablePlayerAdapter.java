package com.fizzycoyote.pockettable.game.poker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fizzycoyote.pockettable.R;
import com.fizzycoyote.pockettable.models.poker.PokerGameState;

import java.util.ArrayList;
import java.util.List;

public class TablePlayerAdapter extends RecyclerView.Adapter<TablePlayerAdapter.ViewHolder> {

    public static class Entry {
        public final PokerGameState.PlayerState player;
        public final boolean isDealer;
        public final boolean isCurrentTurn;
        public final boolean isMe;

        public Entry(PokerGameState.PlayerState player, boolean isDealer, boolean isCurrentTurn, boolean isMe) {
            this.player = player;
            this.isDealer = isDealer;
            this.isCurrentTurn = isCurrentTurn;
            this.isMe = isMe;
        }
    }

    private List<Entry> entries = new ArrayList<>();

    public void updateEntries(List<Entry> newEntries) {
        this.entries = newEntries;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_table_player, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Entry entry = entries.get(position);
        PokerGameState.PlayerState p = entry.player;

        String name = p.playerName() != null ? p.playerName() : holder.itemView.getContext().getString(R.string.player_name_default);
        if (entry.isMe) {
            name = name + " " + holder.itemView.getContext().getString(R.string.you_suffix);
        }
        holder.tvName.setText(name);
        holder.tvChips.setText(String.valueOf(p.chips()));

        holder.tvBadge.setVisibility(entry.isDealer ? View.VISIBLE : View.GONE);

        if (p.folded()) {
            holder.tvStatus.setVisibility(View.VISIBLE);
            holder.tvStatus.setText(holder.itemView.getContext().getString(R.string.status_folded));
            holder.tvStatus.setTextColor(0xFF888888);
            holder.root.setAlpha(0.5f);
        } else if (p.allIn()) {
            holder.tvStatus.setVisibility(View.VISIBLE);
            holder.tvStatus.setText(holder.itemView.getContext().getString(R.string.status_all_in));
            holder.tvStatus.setTextColor(0xFFF44336);
            holder.root.setAlpha(1f);
        } else {
            holder.tvStatus.setVisibility(View.GONE);
            holder.root.setAlpha(1f);
        }

        holder.root.setBackgroundResource(
                entry.isCurrentTurn ? R.drawable.bg_player_card_active : R.drawable.bg_player_card
        );
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout root;
        TextView tvBadge, tvName, tvChips, tvStatus;

        ViewHolder(View itemView) {
            super(itemView);
            root = (LinearLayout) itemView;
            tvBadge = itemView.findViewById(R.id.tvBadge);
            tvName = itemView.findViewById(R.id.tvPlayerName);
            tvChips = itemView.findViewById(R.id.tvPlayerChips);
            tvStatus = itemView.findViewById(R.id.tvPlayerStatus);
        }
    }
}