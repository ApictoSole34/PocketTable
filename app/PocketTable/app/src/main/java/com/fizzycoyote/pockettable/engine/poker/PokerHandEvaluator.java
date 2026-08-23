package com.fizzycoyote.pockettable.engine.poker;

import com.fizzycoyote.pockettable.engine.common.Card;
import com.fizzycoyote.pockettable.engine.common.CardRank;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PokerHandEvaluator {

    public PokerHand evaluate(List<Card> cards) {

        if (cards.size() != 5) {
            throw new IllegalArgumentException(
                    "Exactly 5 cards are required"
            );
        }

        Map<CardRank, Long> rankCounts = cards.stream()
                .collect(Collectors.groupingBy(
                        Card::rank,
                        Collectors.counting()
                ));

        // FOUR OF A KIND
        if (rankCounts.containsValue(4L)) {

            return new PokerHand(
                    PokerHandRank.FOUR_OF_A_KIND,
                    cards
            );
        }

        // FULL HOUSE
        boolean threeOfAKind = rankCounts.containsValue(3L);
        boolean pair = rankCounts.containsValue(2L);

        if (threeOfAKind && pair) {

            return new PokerHand(
                    PokerHandRank.FULL_HOUSE,
                    cards
            );
        }

        // Check FLUSH
        boolean flush = cards.stream()
                .map(Card::suit)
                .distinct()
                .count() == 1;

        // Check STRAIGHT
        List<Integer> rankValues = cards.stream()
                .map(card -> card.rank().ordinal())
                .sorted()
                .collect(Collectors.toList());

        boolean aceLowStraight =
                rankValues.equals(
                        List.of(
                                CardRank.TWO.ordinal(),
                                CardRank.THREE.ordinal(),
                                CardRank.FOUR.ordinal(),
                                CardRank.FIVE.ordinal(),
                                CardRank.ACE.ordinal()
                        )
                );

        boolean straight = aceLowStraight;

        if (!straight) {

            straight = true;

            for (int i = 1; i < rankValues.size(); i++) {

                if (rankValues.get(i) != rankValues.get(i - 1) + 1) {
                    straight = false;
                    break;
                }
            }
        }

        // STRAIGHT FLUSH
        if (straight && flush) {

            return new PokerHand(
                    PokerHandRank.STRAIGHT_FLUSH,
                    cards
            );
        }

        // FLUSH
        if (flush) {

            return new PokerHand(
                    PokerHandRank.FLUSH,
                    cards
            );
        }

        // STRAIGHT
        if (straight) {

            return new PokerHand(
                    PokerHandRank.STRAIGHT,
                    cards
            );
        }

        // THREE OF A KIND
        if (threeOfAKind) {

            return new PokerHand(
                    PokerHandRank.THREE_OF_A_KIND,
                    cards
            );
        }

        // TWO PAIR
        long pairs = rankCounts.values().stream()
                .filter(count -> count == 2)
                .count();

        if (pairs == 2) {

            return new PokerHand(
                    PokerHandRank.TWO_PAIR,
                    cards
            );
        }

        // PAIR
        if (pairs == 1) {

            return new PokerHand(
                    PokerHandRank.PAIR,
                    cards
            );
        }

        // HIGH CARD
        List<Card> sortedCards = cards.stream()
                .sorted(
                        Comparator.comparing(Card::rank).reversed()
                )
                .collect(Collectors.toList());

        return new PokerHand(
                PokerHandRank.HIGH_CARD,
                sortedCards
        );
    }

    public PokerHand evaluateBest(List<Card> cards) {

        if (cards.size() != 7) {
            throw new IllegalArgumentException(
                    "Exactly 7 cards are required"
            );
        }

        PokerHand bestHand = null;

        for (int a = 0; a < cards.size(); a++) {
            for (int b = a + 1; b < cards.size(); b++) {
                for (int c = b + 1; c < cards.size(); c++) {
                    for (int d = c + 1; d < cards.size(); d++) {
                        for (int e = d + 1; e < cards.size(); e++) {

                            List<Card> combination = List.of(
                                    cards.get(a),
                                    cards.get(b),
                                    cards.get(c),
                                    cards.get(d),
                                    cards.get(e)
                            );

                            PokerHand hand = evaluate(combination);

                            if (bestHand == null || hand.compareTo(bestHand) > 0) {
                                bestHand = hand;
                            }
                        }
                    }
                }
            }
        }

        return bestHand;
    }
}