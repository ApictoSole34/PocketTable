package com.fizzycoyote.pockettable.engine.poker;

import com.fizzycoyote.pockettable.engine.common.Card;
import com.fizzycoyote.pockettable.engine.common.CardRank;
import com.fizzycoyote.pockettable.engine.common.CardSuit;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PokerHandTest {

    private Card c(CardSuit suit, CardRank rank) {
        return new Card(suit, rank);
    }

    @Test
    public void higherRankTypeAlwaysWins_regardlessOfCardValues() {
        PokerHand twoPairLow = new PokerHand(PokerHandRank.TWO_PAIR, List.of(
                c(CardSuit.HEARTS, CardRank.TWO), c(CardSuit.DIAMONDS, CardRank.TWO),
                c(CardSuit.CLUBS, CardRank.THREE), c(CardSuit.SPADES, CardRank.THREE),
                c(CardSuit.HEARTS, CardRank.FOUR)
        ));

        PokerHand pairHigh = new PokerHand(PokerHandRank.PAIR, List.of(
                c(CardSuit.HEARTS, CardRank.KING), c(CardSuit.DIAMONDS, CardRank.KING),
                c(CardSuit.CLUBS, CardRank.ACE), c(CardSuit.SPADES, CardRank.QUEEN),
                c(CardSuit.HEARTS, CardRank.JACK)
        ));

        assertTrue(twoPairLow.compareTo(pairHigh) > 0);
        assertTrue(pairHigh.compareTo(twoPairLow) < 0);
    }

    @Test
    public void highCard_comparesKickersInOrder() {
        PokerHand handA = new PokerHand(PokerHandRank.HIGH_CARD, List.of(
                c(CardSuit.HEARTS, CardRank.ACE), c(CardSuit.DIAMONDS, CardRank.KING),
                c(CardSuit.CLUBS, CardRank.QUEEN), c(CardSuit.SPADES, CardRank.JACK),
                c(CardSuit.HEARTS, CardRank.NINE)
        ));

        PokerHand handB = new PokerHand(PokerHandRank.HIGH_CARD, List.of(
                c(CardSuit.HEARTS, CardRank.ACE), c(CardSuit.DIAMONDS, CardRank.KING),
                c(CardSuit.CLUBS, CardRank.QUEEN), c(CardSuit.SPADES, CardRank.JACK),
                c(CardSuit.HEARTS, CardRank.EIGHT)
        ));

        assertTrue(handA.compareTo(handB) > 0);
    }

    @Test
    public void twoPair_comparesKickerWhenBothPairsEqual() {
        PokerHand handA = new PokerHand(PokerHandRank.TWO_PAIR, List.of(
                c(CardSuit.HEARTS, CardRank.ACE), c(CardSuit.DIAMONDS, CardRank.ACE),
                c(CardSuit.CLUBS, CardRank.KING), c(CardSuit.SPADES, CardRank.KING),
                c(CardSuit.HEARTS, CardRank.QUEEN)
        ));

        PokerHand handB = new PokerHand(PokerHandRank.TWO_PAIR, List.of(
                c(CardSuit.HEARTS, CardRank.ACE), c(CardSuit.DIAMONDS, CardRank.ACE),
                c(CardSuit.CLUBS, CardRank.KING), c(CardSuit.SPADES, CardRank.KING),
                c(CardSuit.HEARTS, CardRank.JACK)
        ));

        assertTrue(handA.compareTo(handB) > 0);
    }

    @Test
    public void fullHouse_comparesTripleRankBeforePairRank() {
        PokerHand kingsFull = new PokerHand(PokerHandRank.FULL_HOUSE, List.of(
                c(CardSuit.HEARTS, CardRank.KING), c(CardSuit.DIAMONDS, CardRank.KING), c(CardSuit.CLUBS, CardRank.KING),
                c(CardSuit.SPADES, CardRank.TWO), c(CardSuit.HEARTS, CardRank.TWO)
        ));

        PokerHand queensFull = new PokerHand(PokerHandRank.FULL_HOUSE, List.of(
                c(CardSuit.HEARTS, CardRank.QUEEN), c(CardSuit.DIAMONDS, CardRank.QUEEN), c(CardSuit.CLUBS, CardRank.QUEEN),
                c(CardSuit.SPADES, CardRank.ACE), c(CardSuit.HEARTS, CardRank.ACE)
        ));

        assertTrue(kingsFull.compareTo(queensFull) > 0);
    }

    @Test
    public void fourOfAKind_comparesKickerWhenQuadsEqual() {
        PokerHand handA = new PokerHand(PokerHandRank.FOUR_OF_A_KIND, List.of(
                c(CardSuit.HEARTS, CardRank.FIVE), c(CardSuit.DIAMONDS, CardRank.FIVE),
                c(CardSuit.CLUBS, CardRank.FIVE), c(CardSuit.SPADES, CardRank.FIVE),
                c(CardSuit.HEARTS, CardRank.KING)
        ));

        PokerHand handB = new PokerHand(PokerHandRank.FOUR_OF_A_KIND, List.of(
                c(CardSuit.HEARTS, CardRank.FIVE), c(CardSuit.DIAMONDS, CardRank.FIVE),
                c(CardSuit.CLUBS, CardRank.FIVE), c(CardSuit.SPADES, CardRank.FIVE),
                c(CardSuit.HEARTS, CardRank.QUEEN)
        ));

        assertTrue(handA.compareTo(handB) > 0);
    }

    @Test
    public void straight_higherTopCardWins() {
        PokerHand fiveToNine = new PokerHand(PokerHandRank.STRAIGHT, List.of(
                c(CardSuit.HEARTS, CardRank.FIVE), c(CardSuit.DIAMONDS, CardRank.SIX),
                c(CardSuit.CLUBS, CardRank.SEVEN), c(CardSuit.SPADES, CardRank.EIGHT),
                c(CardSuit.HEARTS, CardRank.NINE)
        ));

        PokerHand aceLow = new PokerHand(PokerHandRank.STRAIGHT, List.of(
                c(CardSuit.HEARTS, CardRank.ACE), c(CardSuit.DIAMONDS, CardRank.TWO),
                c(CardSuit.CLUBS, CardRank.THREE), c(CardSuit.SPADES, CardRank.FOUR),
                c(CardSuit.HEARTS, CardRank.FIVE)
        ));

        assertTrue(fiveToNine.compareTo(aceLow) > 0);
    }

    @Test
    public void straight_aceHighBroadwayBeatsLowerStraight() {
        PokerHand broadway = new PokerHand(PokerHandRank.STRAIGHT, List.of(
                c(CardSuit.HEARTS, CardRank.TEN), c(CardSuit.DIAMONDS, CardRank.JACK),
                c(CardSuit.CLUBS, CardRank.QUEEN), c(CardSuit.SPADES, CardRank.KING),
                c(CardSuit.HEARTS, CardRank.ACE)
        ));

        PokerHand fiveToNine = new PokerHand(PokerHandRank.STRAIGHT, List.of(
                c(CardSuit.HEARTS, CardRank.FIVE), c(CardSuit.DIAMONDS, CardRank.SIX),
                c(CardSuit.CLUBS, CardRank.SEVEN), c(CardSuit.SPADES, CardRank.EIGHT),
                c(CardSuit.HEARTS, CardRank.NINE)
        ));

        assertTrue(broadway.compareTo(fiveToNine) > 0);
    }

    @Test
    public void identicalHands_areEqual() {
        PokerHand handA = new PokerHand(PokerHandRank.TWO_PAIR, List.of(
                c(CardSuit.HEARTS, CardRank.ACE), c(CardSuit.DIAMONDS, CardRank.ACE),
                c(CardSuit.CLUBS, CardRank.KING), c(CardSuit.SPADES, CardRank.KING),
                c(CardSuit.HEARTS, CardRank.QUEEN)
        ));

        PokerHand handB = new PokerHand(PokerHandRank.TWO_PAIR, List.of(
                c(CardSuit.SPADES, CardRank.ACE), c(CardSuit.CLUBS, CardRank.ACE),
                c(CardSuit.HEARTS, CardRank.KING), c(CardSuit.DIAMONDS, CardRank.KING),
                c(CardSuit.SPADES, CardRank.QUEEN)
        ));

        assertEquals(0, handA.compareTo(handB));
    }
}