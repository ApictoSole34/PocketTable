package com.fizzycoyote.pockettable.engine.poker;

import com.fizzycoyote.pockettable.engine.common.Card;
import com.fizzycoyote.pockettable.engine.common.CardRank;
import com.fizzycoyote.pockettable.engine.common.CardSuit;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class PokerHandEvaluatorTest {

    private final PokerHandEvaluator evaluator = new PokerHandEvaluator();

    private Card c(CardSuit suit, CardRank rank) {
        return new Card(suit, rank);
    }

    @Test
    public void highCard_whenNoCombination() {
        List<Card> hand = List.of(
                c(CardSuit.HEARTS, CardRank.TWO),
                c(CardSuit.DIAMONDS, CardRank.FIVE),
                c(CardSuit.CLUBS, CardRank.NINE),
                c(CardSuit.HEARTS, CardRank.JACK),
                c(CardSuit.SPADES, CardRank.KING)
        );

        assertEquals(PokerHandRank.HIGH_CARD, evaluator.evaluate(hand).rank());
    }

    @Test
    public void pair_whenTwoCardsSameRank() {
        List<Card> hand = List.of(
                c(CardSuit.HEARTS, CardRank.TWO),
                c(CardSuit.DIAMONDS, CardRank.TWO),
                c(CardSuit.CLUBS, CardRank.FIVE),
                c(CardSuit.SPADES, CardRank.NINE),
                c(CardSuit.HEARTS, CardRank.KING)
        );

        assertEquals(PokerHandRank.PAIR, evaluator.evaluate(hand).rank());
    }

    @Test
    public void twoPair_whenTwoDifferentPairs() {
        List<Card> hand = List.of(
                c(CardSuit.HEARTS, CardRank.TWO),
                c(CardSuit.DIAMONDS, CardRank.TWO),
                c(CardSuit.CLUBS, CardRank.FIVE),
                c(CardSuit.SPADES, CardRank.FIVE),
                c(CardSuit.HEARTS, CardRank.KING)
        );

        assertEquals(PokerHandRank.TWO_PAIR, evaluator.evaluate(hand).rank());
    }

    @Test
    public void threeOfAKind_whenNoAdditionalPair() {
        List<Card> hand = List.of(
                c(CardSuit.HEARTS, CardRank.TWO),
                c(CardSuit.DIAMONDS, CardRank.TWO),
                c(CardSuit.CLUBS, CardRank.TWO),
                c(CardSuit.SPADES, CardRank.FIVE),
                c(CardSuit.HEARTS, CardRank.KING)
        );

        assertEquals(PokerHandRank.THREE_OF_A_KIND, evaluator.evaluate(hand).rank());
    }

    @Test
    public void straight_normalSequence() {
        List<Card> hand = List.of(
                c(CardSuit.HEARTS, CardRank.FIVE),
                c(CardSuit.DIAMONDS, CardRank.SIX),
                c(CardSuit.CLUBS, CardRank.SEVEN),
                c(CardSuit.SPADES, CardRank.EIGHT),
                c(CardSuit.HEARTS, CardRank.NINE)
        );

        assertEquals(PokerHandRank.STRAIGHT, evaluator.evaluate(hand).rank());
    }

    @Test
    public void straight_aceLow() {
        List<Card> hand = List.of(
                c(CardSuit.HEARTS, CardRank.ACE),
                c(CardSuit.DIAMONDS, CardRank.TWO),
                c(CardSuit.CLUBS, CardRank.THREE),
                c(CardSuit.SPADES, CardRank.FOUR),
                c(CardSuit.HEARTS, CardRank.FIVE)
        );

        assertEquals(PokerHandRank.STRAIGHT, evaluator.evaluate(hand).rank());
    }

    @Test
    public void straight_aceHighBroadway() {
        List<Card> hand = List.of(
                c(CardSuit.HEARTS, CardRank.TEN),
                c(CardSuit.DIAMONDS, CardRank.JACK),
                c(CardSuit.CLUBS, CardRank.QUEEN),
                c(CardSuit.SPADES, CardRank.KING),
                c(CardSuit.HEARTS, CardRank.ACE)
        );

        assertEquals(PokerHandRank.STRAIGHT, evaluator.evaluate(hand).rank());
    }

    @Test
    public void flush_whenAllSameSuitNotConsecutive() {
        List<Card> hand = List.of(
                c(CardSuit.HEARTS, CardRank.TWO),
                c(CardSuit.HEARTS, CardRank.FIVE),
                c(CardSuit.HEARTS, CardRank.NINE),
                c(CardSuit.HEARTS, CardRank.JACK),
                c(CardSuit.HEARTS, CardRank.KING)
        );

        assertEquals(PokerHandRank.FLUSH, evaluator.evaluate(hand).rank());
    }

    @Test
    public void fullHouse_whenTripleAndPair() {
        List<Card> hand = List.of(
                c(CardSuit.HEARTS, CardRank.TWO),
                c(CardSuit.DIAMONDS, CardRank.TWO),
                c(CardSuit.CLUBS, CardRank.TWO),
                c(CardSuit.SPADES, CardRank.FIVE),
                c(CardSuit.HEARTS, CardRank.FIVE)
        );

        assertEquals(PokerHandRank.FULL_HOUSE, evaluator.evaluate(hand).rank());
    }

    @Test
    public void fourOfAKind_whenFourSameRank() {
        List<Card> hand = List.of(
                c(CardSuit.HEARTS, CardRank.TWO),
                c(CardSuit.DIAMONDS, CardRank.TWO),
                c(CardSuit.CLUBS, CardRank.TWO),
                c(CardSuit.SPADES, CardRank.TWO),
                c(CardSuit.HEARTS, CardRank.FIVE)
        );

        assertEquals(PokerHandRank.FOUR_OF_A_KIND, evaluator.evaluate(hand).rank());
    }

    @Test
    public void straightFlush_normalSequenceSameSuit() {
        List<Card> hand = List.of(
                c(CardSuit.HEARTS, CardRank.FIVE),
                c(CardSuit.HEARTS, CardRank.SIX),
                c(CardSuit.HEARTS, CardRank.SEVEN),
                c(CardSuit.HEARTS, CardRank.EIGHT),
                c(CardSuit.HEARTS, CardRank.NINE)
        );

        assertEquals(PokerHandRank.STRAIGHT_FLUSH, evaluator.evaluate(hand).rank());
    }

    @Test
    public void straightFlush_royalFlush() {
        List<Card> hand = List.of(
                c(CardSuit.HEARTS, CardRank.TEN),
                c(CardSuit.HEARTS, CardRank.JACK),
                c(CardSuit.HEARTS, CardRank.QUEEN),
                c(CardSuit.HEARTS, CardRank.KING),
                c(CardSuit.HEARTS, CardRank.ACE)
        );

        assertEquals(PokerHandRank.STRAIGHT_FLUSH, evaluator.evaluate(hand).rank());
    }

    @Test
    public void straightFlush_aceLowSameSuit() {
        List<Card> hand = List.of(
                c(CardSuit.HEARTS, CardRank.ACE),
                c(CardSuit.HEARTS, CardRank.TWO),
                c(CardSuit.HEARTS, CardRank.THREE),
                c(CardSuit.HEARTS, CardRank.FOUR),
                c(CardSuit.HEARTS, CardRank.FIVE)
        );

        assertEquals(PokerHandRank.STRAIGHT_FLUSH, evaluator.evaluate(hand).rank());
    }

    @Test
    public void evaluate_throwsWhenNotExactlyFiveCards() {
        List<Card> tooFew = List.of(
                c(CardSuit.HEARTS, CardRank.TWO),
                c(CardSuit.DIAMONDS, CardRank.THREE)
        );

        assertThrows(IllegalArgumentException.class, () -> evaluator.evaluate(tooFew));
    }

    @Test
    public void evaluateBest_findsStraightOverPairAmongSevenCards() {
        List<Card> sevenCards = List.of(
                c(CardSuit.DIAMONDS, CardRank.NINE),
                c(CardSuit.SPADES, CardRank.NINE),
                c(CardSuit.HEARTS, CardRank.TWO),
                c(CardSuit.DIAMONDS, CardRank.THREE),
                c(CardSuit.CLUBS, CardRank.FOUR),
                c(CardSuit.SPADES, CardRank.FIVE),
                c(CardSuit.HEARTS, CardRank.SIX)
        );

        assertEquals(PokerHandRank.STRAIGHT, evaluator.evaluateBest(sevenCards).rank());
    }

    @Test
    public void evaluateBest_findsFullHouseAmongSevenCards() {
        List<Card> sevenCards = List.of(
                c(CardSuit.HEARTS, CardRank.THREE),
                c(CardSuit.DIAMONDS, CardRank.THREE),
                c(CardSuit.CLUBS, CardRank.THREE),
                c(CardSuit.HEARTS, CardRank.SEVEN),
                c(CardSuit.DIAMONDS, CardRank.SEVEN),
                c(CardSuit.CLUBS, CardRank.NINE),
                c(CardSuit.SPADES, CardRank.TWO)
        );

        assertEquals(PokerHandRank.FULL_HOUSE, evaluator.evaluateBest(sevenCards).rank());
    }

    @Test
    public void evaluateBest_throwsWhenNotExactlySevenCards() {
        List<Card> sixCards = List.of(
                c(CardSuit.HEARTS, CardRank.TWO),
                c(CardSuit.DIAMONDS, CardRank.THREE),
                c(CardSuit.CLUBS, CardRank.FOUR),
                c(CardSuit.SPADES, CardRank.FIVE),
                c(CardSuit.HEARTS, CardRank.SIX),
                c(CardSuit.DIAMONDS, CardRank.SEVEN)
        );

        assertThrows(IllegalArgumentException.class, () -> evaluator.evaluateBest(sixCards));
    }
}