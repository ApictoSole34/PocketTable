package com.pockettable.server.service.game.poker;

import com.pockettable.server.model.game.Card;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PokerPlayer {

    private final UUID playerId;
    private final List<Card> hand = new ArrayList<>();

    public PokerPlayer(UUID playerId) {
        this.playerId = playerId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public List<Card> getHand() {
        return List.copyOf(hand);
    }

    public void addCard(Card card) {
        hand.add(card);
    }
}
