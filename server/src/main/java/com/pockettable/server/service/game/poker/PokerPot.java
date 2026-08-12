package com.pockettable.server.service.game.poker;

public class PokerPot {

    private int amount = 0;

    public int getAmount() {
        return amount;
    }

    public void add(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException(
                    "Amount must be positive"
            );
        }

        this.amount += amount;
    }

    public void reset() {
        this.amount = 0;
    }
}