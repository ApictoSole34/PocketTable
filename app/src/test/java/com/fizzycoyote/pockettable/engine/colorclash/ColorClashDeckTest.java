package com.fizzycoyote.pockettable.engine.colorclash;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ColorClashDeckTest {

    @Test
    public void deck_has108Cards() {
        ColorClashDeck deck = new ColorClashDeck();
        assertEquals(108, deck.size());
    }

    @Test
    public void draw_reducesSizeByOne() {
        ColorClashDeck deck = new ColorClashDeck();
        deck.draw();
        assertEquals(107, deck.size());
    }

    @Test
    public void draw_returnsCard() {
        ColorClashDeck deck = new ColorClashDeck();
        ColorClashCard card = deck.draw();
        assertNotNull(card);
    }

    @Test(expected = IllegalStateException.class)
    public void draw_emptyDeck_throwsException() {
        ColorClashDeck deck = new ColorClashDeck();
        for (int i = 0; i < 108; i++) {
            deck.draw();
        }
        deck.draw();
    }

    @Test
    public void shuffle_doesNotChangeSize() {
        ColorClashDeck deck = new ColorClashDeck();
        int sizeBefore = deck.size();
        deck.shuffle();
        assertEquals(sizeBefore, deck.size());
    }

    @Test
    public void addCard_increasesSize() {
        ColorClashDeck deck = new ColorClashDeck();
        ColorClashCard card = deck.draw();
        int sizeBefore = deck.size();
        deck.addCard(card);
        assertEquals(sizeBefore + 1, deck.size());
    }

    @Test
    public void refillFrom_addsAllCardsAndShuffles() {
        ColorClashDeck deck = new ColorClashDeck();
        List<ColorClashCard> cards = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            cards.add(deck.draw());
        }
        int sizeBefore = deck.size();
        deck.refillFrom(cards);
        assertEquals(sizeBefore + 10, deck.size());
    }

    @Test
    public void reset_restoresFullDeck() {
        ColorClashDeck deck = new ColorClashDeck();
        for (int i = 0; i < 20; i++) {
            deck.draw();
        }
        deck.reset();
        assertEquals(108, deck.size());
    }

    @Test
    public void deck_hasCorrectNumberOfEachType() {
        ColorClashDeck deck = new ColorClashDeck();
        int wildCount = 0, wildDrawFourCount = 0;
        int total = 0;

        while (!deck.isEmpty()) {
            ColorClashCard card = deck.draw();
            total++;
            if (card.type() == CardType.WILD) wildCount++;
            if (card.type() == CardType.WILD_DRAW_FOUR) wildDrawFourCount++;
        }

        assertEquals(108, total);
        assertEquals(4, wildCount);
        assertEquals(4, wildDrawFourCount);
    }
}