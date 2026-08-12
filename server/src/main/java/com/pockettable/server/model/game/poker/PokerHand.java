package com.pockettable.server.model.game.poker;

import com.pockettable.server.model.game.Card;
import com.pockettable.server.model.game.CardRank;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record PokerHand(
        PokerHandRank rank,
        List<Card> cards
) implements Comparable<PokerHand> {

    @Override
    public int compareTo(PokerHand other) {

        int rankComparison =
                this.rank.compareTo(other.rank);

        if (rankComparison != 0) {
            return rankComparison;
        }

        return compareCards(other);
    }

    private int compareCards(PokerHand other) {

        List<Card> thisCards = this.cards.stream()
                .sorted(Comparator.comparing(Card::rank).reversed())
                .toList();

        List<Card> otherCards = other.cards.stream()
                .sorted(Comparator.comparing(Card::rank).reversed())
                .toList();

        return compareRankCounts(thisCards, otherCards);
    }

    private int compareRankCounts(
            List<Card> thisCards,
            List<Card> otherCards
    ) {

        Map<CardRank, Long> thisCounts = thisCards.stream()
                .collect(Collectors.groupingBy(
                        Card::rank,
                        Collectors.counting()
                ));

        Map<CardRank, Long> otherCounts = otherCards.stream()
                .collect(Collectors.groupingBy(
                        Card::rank,
                        Collectors.counting()
                ));

        List<CardRank> thisRanks = thisCounts.entrySet().stream()
                .sorted(
                        Comparator
                                .<Map.Entry<CardRank, Long>>comparingLong(
                                        Map.Entry::getValue
                                )
                                .thenComparing(
                                        Map.Entry::getKey
                                )
                                .reversed()
                )
                .map(Map.Entry::getKey)
                .toList();

        List<CardRank> otherRanks = otherCounts.entrySet().stream()
                .sorted(
                        Comparator
                                .<Map.Entry<CardRank, Long>>comparingLong(
                                        Map.Entry::getValue
                                )
                                .thenComparing(
                                        Map.Entry::getKey
                                )
                                .reversed()
                )
                .map(Map.Entry::getKey)
                .toList();

        for (int i = 0; i < thisRanks.size(); i++) {

            int comparison = thisRanks.get(i)
                    .compareTo(otherRanks.get(i));

            if (comparison != 0) {
                return comparison;
            }
        }

        return 0;
    }
}