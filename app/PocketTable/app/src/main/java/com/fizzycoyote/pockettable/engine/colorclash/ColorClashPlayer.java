package com.fizzycoyote.pockettable.engine.colorclash;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A player in a Color Clash game: their hand, name, and whether they've
 * called "Last Card" after being reduced
 * to a single card.
 */
public class ColorClashPlayer {

    private final UUID playerId;
    private String playerName;
    private final List<ColorClashCard> hand = new ArrayList<>();
    private boolean calledLastCard = false;
    private boolean eliminated = false;

    public ColorClashPlayer(UUID playerId) {
        this.playerId = playerId;
        this.playerName = "Player " + playerId;
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
            throw new IllegalArgumentException("Player does not hold this card");
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