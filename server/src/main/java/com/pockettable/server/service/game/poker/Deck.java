package com.pockettable.server.service.game.poker;

import com.pockettable.server.model.game.Card;
import com.pockettable.server.model.game.CardRank;
import com.pockettable.server.model.game.CardSuit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Deck {

    private final List<Card> cards = new ArrayList<>();

    public Deck() {

        for (CardSuit suit : CardSuit.values()) {
            for (CardRank rank : CardRank.values()) {
                cards.add(new Card(suit, rank));
            }
        }
    }

    public void shuffle() {
        Collections.shuffle(cards);
    }

    public Card draw() {
        if (cards.isEmpty()) {
            throw new IllegalStateException("No cards in Deck");
        }

        return cards.remove(cards.size() - 1);
    }

    public int size() {
        return cards.size();
    }
}
