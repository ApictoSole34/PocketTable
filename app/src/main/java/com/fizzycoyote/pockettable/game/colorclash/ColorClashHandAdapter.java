package com.fizzycoyote.pockettable.game.colorclash;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fizzycoyote.pockettable.R;
import com.fizzycoyote.pockettable.engine.colorclash.ColorClashCard;

import java.util.ArrayList;
import java.util.List;

public class ColorClashHandAdapter extends RecyclerView.Adapter<ColorClashHandAdapter.ViewHolder> {

    private List<ColorClashCard> cards = new ArrayList<>();
    private final OnCardClickListener listener;

    public interface OnCardClickListener {
        void onCardClick(ColorClashCard card);
    }

    public ColorClashHandAdapter(List<ColorClashCard> cards, OnCardClickListener listener) {
        this.cards = cards;
        this.listener = listener;
    }

    public void updateCards(List<ColorClashCard> newCards) {
        this.cards = newCards;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_colorclash_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ColorClashCard card = cards.get(position);
        int resId = ColorClashCardResourceHelper.getCardResource(holder.itemView.getContext(), card);
        holder.imageView.setImageResource(resId);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onCardClick(card);
        });
    }

    @Override
    public int getItemCount() {
        return cards.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imgCard);
        }
    }
}