package com.pockettable.server.model.game.poker;

import com.pockettable.server.model.game.Card;
import com.pockettable.server.model.game.CardRank;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record PokerHand(PokerHandRank rank, List<Card> cards) implements Comparable<PokerHand> {

    public PokerHand {

        List<Card> sorted = new ArrayList<>(cards);
        sorted.sort(Comparator.comparing(Card::rank).reversed());
        cards = List.copyOf(sorted);
    }

    @Override
    public int compareTo(PokerHand other) {

        int rankComp = this.rank.compareTo(other.rank);
        if (rankComp != 0) {
            return rankComp;
        }

        if (this.rank == PokerHandRank.STRAIGHT || this.rank == PokerHandRank.STRAIGHT_FLUSH) {
            CardRank thisHigh = getStraightHigh(this.cards);
            CardRank otherHigh = getStraightHigh(other.cards);
            return thisHigh.compareTo(otherHigh);
        }

        List<CardRank> thisOrder = getRanksByFrequency(this.cards);
        List<CardRank> otherOrder = getRanksByFrequency(other.cards);

        for (int i = 0; i < thisOrder.size(); i++) {
            int comp = thisOrder.get(i).compareTo(otherOrder.get(i));
            if (comp != 0) {
                return comp;
            }
        }

        return 0;
    }

    private CardRank getStraightHigh(List<Card> cards) {
        boolean hasAce = cards.stream().anyMatch(c -> c.rank() == CardRank.ACE);
        boolean hasTwo = cards.stream().anyMatch(c -> c.rank() == CardRank.TWO);
        boolean hasThree = cards.stream().anyMatch(c -> c.rank() == CardRank.THREE);
        boolean hasFour = cards.stream().anyMatch(c -> c.rank() == CardRank.FOUR);
        boolean hasFive = cards.stream().anyMatch(c -> c.rank() == CardRank.FIVE);

        if (hasAce && hasTwo && hasThree && hasFour && hasFive) {
            return CardRank.FIVE;
        }

        return cards.stream()
                .map(Card::rank)
                .max(Enum::compareTo)
                .orElseThrow();
    }

    private List<CardRank> getRanksByFrequency(List<Card> cards) {
        Map<CardRank, Long> freq = cards.stream()
                .collect(Collectors.groupingBy(Card::rank, Collectors.counting()));

        return freq.entrySet().stream()
                .sorted(Comparator
                        .<Map.Entry<CardRank, Long>>comparingLong(Map.Entry::getValue)
                        .reversed()
                        .thenComparing(Map.Entry::getKey, Comparator.reverseOrder()))
                .map(Map.Entry::getKey)
                .toList();
    }
}