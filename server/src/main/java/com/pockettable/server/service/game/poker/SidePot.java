package com.pockettable.server.service.game.poker;

import java.util.List;
import java.util.UUID;

public class SidePot {

    private final int amount;
    private final List<UUID> eligiblePlayerIds;

    public SidePot(int amount, List<UUID> eligiblePlayerIds) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        this.amount = amount;
        this.eligiblePlayerIds = List.copyOf(eligiblePlayerIds);
    }

    public int getAmount() {
        return amount;
    }

    public List<UUID> getEligiblePlayerIds() {
        return eligiblePlayerIds;
    }
}