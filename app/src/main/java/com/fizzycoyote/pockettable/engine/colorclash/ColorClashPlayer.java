package com.fizzycoyote.pockettable.engine.colorclash;

import android.content.Context;

import com.fizzycoyote.pockettable.R;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A player in a Color Clash game: their hand, name, and whether they've
 * called "Last Card" after being reduced
 * to a single card.
 */
public class ColorClashPlayer {

    private final Context context;
    private final UUID playerId;
    private String playerName;
    private final List<ColorClashCard> hand = new ArrayList<>();
    private boolean calledLastCard = false;
    private boolean eliminated = false;

    public ColorClashPlayer(Context context, UUID playerId) {
        this.context = (context != null) ? context.getApplicationContext() : null;
        this.playerId = playerId;
        this.playerName = "Player " + playerId;
    }

    public ColorClashPlayer(UUID playerId) {
        this(null, playerId);
    }

    private String getString(int resId, Object... args) {
        if (context != null) {
            return context.getString(resId, args);
        }
        return "?";
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName != null ? playerName : this.playerName;
    }

    public List<ColorClashCard> getHand() {
        return List.copyOf(hand);
    }

    public int getHandSize() {
        return hand.size();
    }

    public void addCard(ColorClashCard card) {
        hand.add(card);
        calledLastCard = false;
    }

    public void addCards(List<ColorClashCard> cards) {
        hand.addAll(cards);
        calledLastCard = false;
    }

    public ColorClashCard removeCard(ColorClashCard card) {
        if (!hand.remove(card)) {
            throw new IllegalArgumentException(getString(R.string.colorclash_error_card_not_held));
        }
        if (hand.size() != 1) {
            calledLastCard = false;
        }
        return card;
    }

    public boolean hasCard(ColorClashCard card) {
        return hand.contains(card);
    }

    public boolean hasWon() {
        return hand.isEmpty();
    }

    public boolean isCalledLastCard() {
        return calledLastCard;
    }

    public void callLastCard() {
        this.calledLastCard = true;
    }

    public void clearLastCardCall() {
        this.calledLastCard = false;
    }

    public boolean isEliminated() {
        return eliminated;
    }

    public void eliminate() {
        this.eliminated = true;
    }

    public void clearHand() {
        hand.clear();
        calledLastCard = false;
    }
}