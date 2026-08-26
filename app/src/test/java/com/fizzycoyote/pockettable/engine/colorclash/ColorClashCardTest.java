package com.fizzycoyote.pockettable.engine.colorclash;

import org.junit.Test;

import static org.junit.Assert.*;

public class ColorClashCardTest {

    @Test
    public void numberCard_hasCorrectFields() {
        ColorClashCard card = ColorClashCard.number(CardColor.RED, 5);
        assertEquals(CardColor.RED, card.color());
        assertEquals(CardType.NUMBER, card.type());
        assertEquals(5, card.value());
        assertFalse(card.isWild());
        assertFalse(card.isActionCard());
    }

    @Test(expected = IllegalArgumentException.class)
    public void numberCard_valueBelowZero_throwsException() {
        ColorClashCard.number(CardColor.BLUE, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void numberCard_valueAboveNine_throwsException() {
        ColorClashCard.number(CardColor.GREEN, 10);
    }

    @Test
    public void skipCard_hasCorrectFields() {
        ColorClashCard card = ColorClashCard.skip(CardColor.YELLOW);
        assertEquals(CardColor.YELLOW, card.color());
        assertEquals(CardType.SKIP, card.type());
        assertEquals(-1, card.value());
        assertFalse(card.isWild());
        assertTrue(card.isActionCard());
    }

    @Test
    public void reverseCard_hasCorrectFields() {
        ColorClashCard card = ColorClashCard.reverse(CardColor.RED);
        assertEquals(CardType.REVERSE, card.type());
        assertTrue(card.isActionCard());
    }

    @Test
    public void drawTwoCard_hasCorrectFields() {
        ColorClashCard card = ColorClashCard.drawTwo(CardColor.BLUE);
        assertEquals(CardType.DRAW_TWO, card.type());
        assertTrue(card.isActionCard());
        assertFalse(card.isWild());
    }

    @Test
    public void wildCard_hasCorrectFields() {
        ColorClashCard card = ColorClashCard.wild();
        assertEquals(CardColor.WILD, card.color());
        assertEquals(CardType.WILD, card.type());
        assertTrue(card.isWild());
        assertTrue(card.isActionCard());
    }

    @Test
    public void wildDrawFourCard_hasCorrectFields() {
        ColorClashCard card = ColorClashCard.wildDrawFour();
        assertEquals(CardColor.WILD, card.color());
        assertEquals(CardType.WILD_DRAW_FOUR, card.type());
        assertTrue(card.isWild());
        assertTrue(card.isActionCard());
    }

    @Test(expected = IllegalArgumentException.class)
    public void nonWildCard_withWildColor_throwsException() {
        new ColorClashCard(CardColor.WILD, CardType.NUMBER, 5);
    }

    @Test
    public void wildCard_doesNotThrowForWildColor() {
        new ColorClashCard(CardColor.WILD, CardType.WILD, -1);
        new ColorClashCard(CardColor.WILD, CardType.WILD_DRAW_FOUR, -1);
    }

    @Test
    public void wild_withColorAfterPlay_isNotConsideredWildByColorCheck() {
        ColorClashCard wildRed = new ColorClashCard(CardColor.RED, CardType.WILD, -1);
        assertTrue(wildRed.isWild());
        assertEquals(CardColor.RED, wildRed.color());
    }
}