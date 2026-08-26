package com.fizzycoyote.pockettable.engine.colorclash;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Standard 108-card Color Clash deck (equivalent to a standard Uno deck):
 * for each of the 4 colors - one 0, two of each 1-9, two Skip, two Reverse,
 * two Draw Two (25 cards per color, 100 total) - plus 4 Wild and 4 Wild Draw
 * Four cards.
 */
public class ColorClashDeck {

    private final List<ColorClashCard> drawPile = new ArrayList<>();

    public ColorClashDeck() {
        buildFullDeck();
        shuffle();
    }

    private void buildFullDeck() {
        drawPile.clear();

        for (CardColor color : List.of(CardColor.RED, CardColor.YELLOW, CardColor.GREEN, CardColor.BLUE)) {
            drawPile.add(ColorClashCard.number(color, 0));
            for (int value = 1; value <= 9; value++) {
                drawPile.add(ColorClashCard.number(color, value));
                drawPile.add(ColorClashCard.number(color, value));
            }
            for (int i = 0; i < 2; i++) {
                drawPile.add(ColorClashCard.skip(color));
                drawPile.add(ColorClashCard.reverse(color));
                drawPile.add(ColorClashCard.drawTwo(color));
            }
        }

        for (int i = 0; i < 4; i++) {
            drawPile.add(ColorClashCard.wild());
            drawPile.add(ColorClashCard.wildDrawFour());
        }
    }

    public void addCardToBottom(ColorClashCard card) {
        drawPile.add(0, card);
    }

    public void shuffle() {
        Collections.shuffle(drawPile);
    }

    public boolean isEmpty() {
        return drawPile.isEmpty();
    }

    public int size() {
        return drawPile.size();
    }

    public ColorClashCard draw() {
        if (drawPile.isEmpty()) {
            throw new IllegalStateException("Draw pile is empty");
        }
        return drawPile.remove(drawPile.size() - 1);
    }

    public void addCard(ColorClashCard card) {
        drawPile.add(card);
    }

    public void refillFrom(List<ColorClashCard> cards) {
        drawPile.addAll(cards);
        shuffle();
    }

    public void reset() {
        buildFullDeck();
        shuffle();
    }
}