package com.pockettable.server.service.game.poker;

import com.pockettable.server.model.game.Card;
import com.pockettable.server.model.game.CardRank;
import com.pockettable.server.model.game.CardSuit;
import com.pockettable.server.model.game.poker.PokerHand;
import com.pockettable.server.model.game.poker.PokerHandRank;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

class PokerHandEvaluatorTest {

    @Test
    void shouldEvaluatePair() {

        PokerHandEvaluator evaluator = new PokerHandEvaluator();

        List<Card> cards = List.of(
                new Card(CardSuit.HEARTS, CardRank.ACE),
                new Card(CardSuit.SPADES, CardRank.ACE),
                new Card(CardSuit.CLUBS, CardRank.KING),
                new Card(CardSuit.DIAMONDS, CardRank.EIGHT),
                new Card(CardSuit.HEARTS, CardRank.THREE)
        );

        PokerHand hand = evaluator.evaluate(cards);

        assertEquals(
                PokerHandRank.PAIR,
                hand.rank()
        );
    }

    @Test
    void shouldEvaluateHighCard() {

        PokerHandEvaluator evaluator = new PokerHandEvaluator();

        List<Card> cards = List.of(
                new Card(CardSuit.HEARTS, CardRank.ACE),
                new Card(CardSuit.SPADES, CardRank.JACK),
                new Card(CardSuit.CLUBS, CardRank.EIGHT),
                new Card(CardSuit.DIAMONDS, CardRank.FIVE),
                new Card(CardSuit.HEARTS, CardRank.TWO)
        );

        PokerHand hand = evaluator.evaluate(cards);

        assertEquals(
                PokerHandRank.HIGH_CARD,
                hand.rank()
        );
    }

    @Test
    void shouldEvaluateTwoPair() {

        PokerHandEvaluator evaluator = new PokerHandEvaluator();

        List<Card> cards = List.of(
                new Card(CardSuit.HEARTS, CardRank.ACE),
                new Card(CardSuit.SPADES, CardRank.ACE),
                new Card(CardSuit.CLUBS, CardRank.KING),
                new Card(CardSuit.DIAMONDS, CardRank.KING),
                new Card(CardSuit.HEARTS, CardRank.THREE)
        );

        PokerHand hand = evaluator.evaluate(cards);

        assertEquals(
                PokerHandRank.TWO_PAIR,
                hand.rank()
        );
    }

    @Test
    void shouldEvaluateThreeOfAKind() {

        PokerHandEvaluator evaluator = new PokerHandEvaluator();

        List<Card> cards = List.of(
                new Card(CardSuit.HEARTS, CardRank.ACE),
                new Card(CardSuit.SPADES, CardRank.ACE),
                new Card(CardSuit.CLUBS, CardRank.ACE),
                new Card(CardSuit.DIAMONDS, CardRank.KING),
                new Card(CardSuit.HEARTS, CardRank.THREE)
        );

        PokerHand hand = evaluator.evaluate(cards);

        assertEquals(
                PokerHandRank.THREE_OF_A_KIND,
                hand.rank()
        );
    }

    @Test
    void shouldEvaluateStraight() {

        PokerHandEvaluator evaluator = new PokerHandEvaluator();

        List<Card> cards = List.of(
                new Card(CardSuit.HEARTS, CardRank.FIVE),
                new Card(CardSuit.SPADES, CardRank.SIX),
                new Card(CardSuit.CLUBS, CardRank.SEVEN),
                new Card(CardSuit.DIAMONDS, CardRank.EIGHT),
                new Card(CardSuit.HEARTS, CardRank.NINE)
        );

        PokerHand hand = evaluator.evaluate(cards);

        assertEquals(
                PokerHandRank.STRAIGHT,
                hand.rank()
        );
    }

    @Test
    void shouldEvaluateFlush() {

        PokerHandEvaluator evaluator = new PokerHandEvaluator();

        List<Card> cards = List.of(
                new Card(CardSuit.HEARTS, CardRank.ACE),
                new Card(CardSuit.HEARTS, CardRank.JACK),
                new Card(CardSuit.HEARTS, CardRank.EIGHT),
                new Card(CardSuit.HEARTS, CardRank.FIVE),
                new Card(CardSuit.HEARTS, CardRank.TWO)
        );

        PokerHand hand = evaluator.evaluate(cards);

        assertEquals(
                PokerHandRank.FLUSH,
                hand.rank()
        );
    }

    @Test
    void shouldEvaluateFullHouse() {

        PokerHandEvaluator evaluator = new PokerHandEvaluator();

        List<Card> cards = List.of(
                new Card(CardSuit.HEARTS, CardRank.ACE),
                new Card(CardSuit.SPADES, CardRank.ACE),
                new Card(CardSuit.CLUBS, CardRank.ACE),
                new Card(CardSuit.DIAMONDS, CardRank.KING),
                new Card(CardSuit.HEARTS, CardRank.KING)
        );

        PokerHand hand = evaluator.evaluate(cards);

        assertEquals(
                PokerHandRank.FULL_HOUSE,
                hand.rank()
        );
    }

    @Test
    void shouldEvaluateFourOfAKind() {

        PokerHandEvaluator evaluator = new PokerHandEvaluator();

        List<Card> cards = List.of(
                new Card(CardSuit.HEARTS, CardRank.ACE),
                new Card(CardSuit.SPADES, CardRank.ACE),
                new Card(CardSuit.CLUBS, CardRank.ACE),
                new Card(CardSuit.DIAMONDS, CardRank.ACE),
                new Card(CardSuit.HEARTS, CardRank.KING)
        );

        PokerHand hand = evaluator.evaluate(cards);

        assertEquals(
                PokerHandRank.FOUR_OF_A_KIND,
                hand.rank()
        );
    }

    @Test
    void shouldEvaluateStraightFlush() {

        PokerHandEvaluator evaluator = new PokerHandEvaluator();

        List<Card> cards = List.of(
                new Card(CardSuit.HEARTS, CardRank.FIVE),
                new Card(CardSuit.HEARTS, CardRank.SIX),
                new Card(CardSuit.HEARTS, CardRank.SEVEN),
                new Card(CardSuit.HEARTS, CardRank.EIGHT),
                new Card(CardSuit.HEARTS, CardRank.NINE)
        );

        PokerHand hand = evaluator.evaluate(cards);

        assertEquals(
                PokerHandRank.STRAIGHT_FLUSH,
                hand.rank()
        );
    }

    @Test
    void shouldChooseBetterPair() {

        PokerHandEvaluator evaluator = new PokerHandEvaluator();

        PokerHand weaker = evaluator.evaluate(List.of(
                new Card(CardSuit.HEARTS, CardRank.KING),
                new Card(CardSuit.SPADES, CardRank.KING),
                new Card(CardSuit.CLUBS, CardRank.ACE),
                new Card(CardSuit.DIAMONDS, CardRank.EIGHT),
                new Card(CardSuit.HEARTS, CardRank.THREE)
        ));

        PokerHand stronger = evaluator.evaluate(List.of(
                new Card(CardSuit.HEARTS, CardRank.ACE),
                new Card(CardSuit.SPADES, CardRank.ACE),
                new Card(CardSuit.CLUBS, CardRank.QUEEN),
                new Card(CardSuit.DIAMONDS, CardRank.EIGHT),
                new Card(CardSuit.HEARTS, CardRank.THREE)
        ));

        assertTrue(stronger.compareTo(weaker) > 0);
    }

    @Test
    void shouldChooseBetterKicker() {

        PokerHandEvaluator evaluator = new PokerHandEvaluator();

        PokerHand weaker = evaluator.evaluate(List.of(
                new Card(CardSuit.HEARTS, CardRank.ACE),
                new Card(CardSuit.SPADES, CardRank.ACE),
                new Card(CardSuit.CLUBS, CardRank.QUEEN),
                new Card(CardSuit.DIAMONDS, CardRank.EIGHT),
                new Card(CardSuit.HEARTS, CardRank.THREE)
        ));

        PokerHand stronger = evaluator.evaluate(List.of(
                new Card(CardSuit.HEARTS, CardRank.ACE),
                new Card(CardSuit.SPADES, CardRank.ACE),
                new Card(CardSuit.CLUBS, CardRank.KING),
                new Card(CardSuit.DIAMONDS, CardRank.EIGHT),
                new Card(CardSuit.HEARTS, CardRank.THREE)
        ));

        assertTrue(stronger.compareTo(weaker) > 0);
    }

    @Test
    void shouldReturnZeroForEqualHands() {

        PokerHandEvaluator evaluator = new PokerHandEvaluator();

        PokerHand hand1 = evaluator.evaluate(List.of(
                new Card(CardSuit.HEARTS, CardRank.ACE),
                new Card(CardSuit.SPADES, CardRank.ACE),
                new Card(CardSuit.CLUBS, CardRank.KING),
                new Card(CardSuit.DIAMONDS, CardRank.EIGHT),
                new Card(CardSuit.HEARTS, CardRank.THREE)
        ));

        PokerHand hand2 = evaluator.evaluate(List.of(
                new Card(CardSuit.CLUBS, CardRank.ACE),
                new Card(CardSuit.DIAMONDS, CardRank.ACE),
                new Card(CardSuit.HEARTS, CardRank.KING),
                new Card(CardSuit.SPADES, CardRank.EIGHT),
                new Card(CardSuit.CLUBS, CardRank.THREE)
        ));

        assertEquals(0, hand1.compareTo(hand2));
    }

    @Test
    void shouldChooseBestHandFromSevenCards() {

        PokerHandEvaluator evaluator = new PokerHandEvaluator();

        List<Card> cards = List.of(
                // karty gracza
                new Card(CardSuit.HEARTS, CardRank.ACE),
                new Card(CardSuit.SPADES, CardRank.ACE),

                // community
                new Card(CardSuit.CLUBS, CardRank.ACE),
                new Card(CardSuit.DIAMONDS, CardRank.KING),
                new Card(CardSuit.HEARTS, CardRank.KING),
                new Card(CardSuit.SPADES, CardRank.THREE),
                new Card(CardSuit.CLUBS, CardRank.TWO)
        );

        PokerHand bestHand = evaluator.evaluateBest(cards);

        assertEquals(
                PokerHandRank.FULL_HOUSE,
                bestHand.rank()
        );
    }

    @Test
    void shouldFindStraightInsideSevenCards() {

        PokerHandEvaluator evaluator = new PokerHandEvaluator();

        List<Card> cards = List.of(
                new Card(CardSuit.HEARTS, CardRank.ACE),
                new Card(CardSuit.SPADES, CardRank.TWO),
                new Card(CardSuit.CLUBS, CardRank.THREE),
                new Card(CardSuit.DIAMONDS, CardRank.FOUR),
                new Card(CardSuit.HEARTS, CardRank.FIVE),
                new Card(CardSuit.SPADES, CardRank.KING),
                new Card(CardSuit.CLUBS, CardRank.QUEEN)
        );

        PokerHand bestHand = evaluator.evaluateBest(cards);

        assertEquals(
                PokerHandRank.STRAIGHT,
                bestHand.rank()
        );
    }

    @Test
    void shouldChooseBetterTwoPair() {

        PokerHandEvaluator evaluator = new PokerHandEvaluator();

        PokerHand weaker = evaluator.evaluate(List.of(
                new Card(CardSuit.HEARTS, CardRank.KING),
                new Card(CardSuit.SPADES, CardRank.KING),
                new Card(CardSuit.CLUBS, CardRank.QUEEN),
                new Card(CardSuit.DIAMONDS, CardRank.QUEEN),
                new Card(CardSuit.HEARTS, CardRank.THREE)
        ));

        PokerHand stronger = evaluator.evaluate(List.of(
                new Card(CardSuit.HEARTS, CardRank.ACE),
                new Card(CardSuit.SPADES, CardRank.ACE),
                new Card(CardSuit.CLUBS, CardRank.TWO),
                new Card(CardSuit.DIAMONDS, CardRank.TWO),
                new Card(CardSuit.HEARTS, CardRank.THREE)
        ));

        assertTrue(stronger.compareTo(weaker) > 0);
    }

    @Test
    void shouldCompareSecondPairInTwoPair() {

        PokerHandEvaluator evaluator = new PokerHandEvaluator();

        PokerHand weaker = evaluator.evaluate(List.of(
                new Card(CardSuit.HEARTS, CardRank.ACE),
                new Card(CardSuit.SPADES, CardRank.ACE),
                new Card(CardSuit.CLUBS, CardRank.THREE),
                new Card(CardSuit.DIAMONDS, CardRank.THREE),
                new Card(CardSuit.HEARTS, CardRank.KING)
        ));

        PokerHand stronger = evaluator.evaluate(List.of(
                new Card(CardSuit.HEARTS, CardRank.ACE),
                new Card(CardSuit.SPADES, CardRank.ACE),
                new Card(CardSuit.CLUBS, CardRank.FOUR),
                new Card(CardSuit.DIAMONDS, CardRank.FOUR),
                new Card(CardSuit.HEARTS, CardRank.KING)
        ));

        assertTrue(stronger.compareTo(weaker) > 0);
    }

    @Test
    void shouldCompareKickerInTwoPair() {

        PokerHandEvaluator evaluator = new PokerHandEvaluator();

        PokerHand weaker = evaluator.evaluate(List.of(
                new Card(CardSuit.HEARTS, CardRank.ACE),
                new Card(CardSuit.SPADES, CardRank.ACE),
                new Card(CardSuit.CLUBS, CardRank.FOUR),
                new Card(CardSuit.DIAMONDS, CardRank.FOUR),
                new Card(CardSuit.HEARTS, CardRank.KING)
        ));

        PokerHand stronger = evaluator.evaluate(List.of(
                new Card(CardSuit.HEARTS, CardRank.ACE),
                new Card(CardSuit.SPADES, CardRank.ACE),
                new Card(CardSuit.CLUBS, CardRank.FOUR),
                new Card(CardSuit.DIAMONDS, CardRank.FOUR),
                new Card(CardSuit.HEARTS, CardRank.QUEEN)
        ));

        assertTrue(weaker.compareTo(stronger) > 0);
    }
}
