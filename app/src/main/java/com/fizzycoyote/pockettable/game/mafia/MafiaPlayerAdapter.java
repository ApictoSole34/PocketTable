package com.fizzycoyote.pockettable.game.mafia;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fizzycoyote.pockettable.R;
import com.fizzycoyote.pockettable.engine.mafia.MafiaRole;
import com.fizzycoyote.pockettable.models.mafia.MafiaState;

import java.util.ArrayList;
import java.util.List;

public class MafiaPlayerAdapter extends RecyclerView.Adapter<MafiaPlayerAdapter.ViewHolder> {

    private List<Entry> entries = new ArrayList<>();
    private final OnPlayerClick listener;

    public interface OnPlayerClick {
        void onClick(MafiaState.PlayerInfo player);
    }

    public MafiaPlayerAdapter(List<Entry> entries, OnPlayerClick listener) {
        this.entries = entries;
        this.listener = listener;
    }

    public void updateEntries(List<Entry> newEntries) {
        this.entries = newEntries;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mafia_player, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Entry entry = entries.get(position);
        MafiaState.PlayerInfo info = entry.info;

        holder.tvName.setText(info.playerName());

        String deadText = holder.itemView.getContext().getString(R.string.mafia_status_dead);
        holder.tvStatus.setText(info.alive() ? "" : deadText);

        int bgRes;
        if (entry.isSelected) {
            bgRes = R.drawable.bg_mafia_player_item_selected;
        } else if (info.alive()) {
            bgRes = R.drawable.bg_mafia_player_item;
        } else {
            bgRes = R.drawable.bg_mafia_player_item_dead;
        }
        holder.itemView.setBackgroundResource(bgRes);

        if (info.role() != null) {
            holder.ivRole.setVisibility(View.VISIBLE);
            holder.ivRole.setImageResource(getIconForRole(info.role()));
        } else {
            holder.ivRole.setVisibility(View.GONE);
        }

        if (entry.voteCount > 0) {
            holder.tvVoted.setVisibility(View.VISIBLE);
            holder.tvVoted.setText(String.valueOf(entry.voteCount));
        } else {
            holder.tvVoted.setVisibility(View.GONE);
        }

        if (entry.isSelected) {
            bgRes = R.drawable.bg_mafia_player_item_selected;
        } else if (info.alive()) {
            bgRes = R.drawable.bg_mafia_player_item;
        } else {
            bgRes = R.drawable.bg_mafia_player_item_dead;
        }

        holder.itemView.setBackgroundResource(bgRes);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onClick(info);
            }
        });
    }

    public static int getIconForRole(MafiaRole role) {
        if (role == null) return R.drawable.icon_mafia_civilian;
        switch (role) {
            case MAFIA: return R.drawable.icon_mafia_mafia;
            case SERIAL_KILLER: return R.drawable.icon_mafia_serialkiller;
            case DETECTIVE: return R.drawable.icon_mafia_detective;
            case DOCTOR: return R.drawable.icon_mafia_doctor;
            case VIGILANTE: return R.drawable.icon_mafia_vigilante;
            case MAYOR: return R.drawable.icon_mafia_mayor;
            case JESTER: return R.drawable.icon_mafia_jester;
            default: return R.drawable.icon_mafia_civilian;
        }
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivRole;
        TextView tvName, tvStatus, tvVoted;
        ViewHolder(View itemView) {
            super(itemView);
            ivRole = itemView.findViewById(R.id.ivRoleIcon);
            tvName = itemView.findViewById(R.id.tvPlayerName);
            tvStatus = itemView.findViewById(R.id.tvPlayerStatus);
            tvVoted = itemView.findViewById(R.id.tvVotedBadge);
        }
    }

    public static class Entry {
        public final MafiaState.PlayerInfo info;
        public boolean isTargetable;
        public boolean isMafiaBrother;
        public boolean isSelected;
        public int voteCount;

        public Entry(MafiaState.PlayerInfo info) {
            this.info = info;
        }
    }
}