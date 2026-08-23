package com.fizzycoyote.pockettable.engine.poker;

import com.fizzycoyote.pockettable.engine.common.Card;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PokerPlayer {

    private final UUID playerId;
    private String playerName;
    private final List<Card> hand = new ArrayList<>();
    private boolean folded = false;
    private int currentBet = 0;
    private boolean acted = false;
    private int chips;
    private boolean allIn = false;
    private int totalContribution = 0;

    public PokerPlayer(UUID playerId, int startingChips) {
        this.playerId = playerId;
        this.playerName = "Player " + playerId;
        this.chips = startingChips;
    }

    public PokerPlayer(UUID playerId) {
        this.playerId = playerId;
        this.playerName = "Player " + playerId;
        this.chips = 1000;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public List<Card> getHand() {
        return List.copyOf(hand);
    }

    public String getPlayerName() {return playerName;}

    public void setPlayerName(String playerName) {this.playerName = playerName != null ? playerName : getPlayerName();}

    public void addCard(Card card) {
        hand.add(card);
    }

    public boolean isFolded() {
        return folded;
    }

    public void fold() {
        this.folded = true;
    }

    public int getCurrentBet() {
        return currentBet;
    }

    public int getChips() {
        return chips;
    }

    public void resetChips(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        this.chips = amount;
    }

    public int getTotalContribution() {return totalContribution;}

    public void resetTotalContribution() {this.totalContribution = 0;}

    public void resetBet() {
        this.currentBet = 0;
    }

    public void addBet(int amount) {

        if (amount <= 0) {
            throw new IllegalArgumentException(
                    "Bet amount must be positive"
            );
        }

        currentBet += amount;
        totalContribution += amount;
    }

    public boolean hasActed() {
        return acted;
    }

    public void markActed() {
        acted = true;
    }

    public void resetActed() {
        acted = false;
    }

    public void resetFolded() {
        folded = false;
    }

    public void clearHand() {
        hand.clear();
    }

    public boolean isAllIn() {
        return allIn;
    }

    public void resetAllIn() {
        this.allIn = false;
    }

    public void removeChips(int amount) {

        if (amount <= 0) {
            throw new IllegalArgumentException(
                    "Amount must be positive"
            );
        }

        if (chips < amount) {
            throw new IllegalStateException(
                    "Not enough chips"
            );
        }

        chips -= amount;
    }

    public int removeChipsUpTo(int amount) {

        if (amount <= 0) {
            throw new IllegalArgumentException(
                    "Amount must be positive"
            );
        }

        int actual = Math.min(amount, chips);
        chips -= actual;

        if (chips == 0) {
            allIn = true;
        }

        return actual;
    }

    public void addChips(int amount) {

        if (amount <= 0) {
            throw new IllegalArgumentException(
                    "Amount must be positive"
            );
        }

        chips += amount;
    }
}