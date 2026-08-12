package com.pockettable.server.service.game.poker;

import com.pockettable.server.model.game.Card;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PokerGame {

    private final List<PokerPlayer> players;
    private final Deck deck;
    private final List<Card> communityCards = new ArrayList<>();

    private PokerRound round = PokerRound.PRE_FLOP;

     public List<Card> getCommunityCards() {
         return List.copyOf(communityCards);
     }

     public PokerRound getRound() {
         return round;
     }

     public void dealFlop() {

         deck.draw();

         for (int i = 0; i < 3; i ++) {
             communityCards.add(deck.draw());
         }

         round = PokerRound.FLOP;
     }

     public void dealRiver() {

         if (round != PokerRound.TURN) {
             throw new IllegalStateException(
                     "Cannot deal river during" + round
             );
         }

         deck.draw();

         communityCards.add(deck.draw());

         round =  PokerRound.RIVER;
     }

     public void dealTurn() {

         if (round != PokerRound.FLOP) {
             throw new IllegalStateException(
                     "Cannot deal turn during " + round
             );
         }

         deck.draw();

         communityCards.add(deck.draw());

         round = PokerRound.TURN;
     }

    public PokerGame(List<UUID> playersIds) {

        this.players = playersIds.stream()
                .map(PokerPlayer::new)
                .collect(Collectors.toCollection(ArrayList::new));

        this.deck = new Deck();
        this.deck.shuffle();
    }

    public List<PokerPlayer> getPlayers() {
        return players;
    }

    public Deck getDeck() {
        return deck;
    }

    public void dealInitialCards() {

        for (int i = 0; i < 2; i++) {

            for (PokerPlayer player : players) {

                player.addCard(deck.draw());
            }
        }
    }
}
