package com.fizzycoyote.pockettable.engine.colorclash;

public record ColorClashCard(CardColor color, CardType type, int value) {

    public ColorClashCard {
        if (type == CardType.NUMBER) {
            if (value < 0 || value > 9) {
                throw new IllegalArgumentException("Number cards must have a value between 0 and 9");
            }
        } else if (value != -1) {
            throw new IllegalArgumentException("Non-number cards must not have a value");
        }
        if (type != CardType.WILD && type != CardType.WILD_DRAW_FOUR && color == CardColor.WILD) {
            throw new IllegalArgumentException("Only wild cards may have WILD as their color");
        }
    }

    public static ColorClashCard number(CardColor color, int value) {
        return new ColorClashCard(color, CardType.NUMBER, value);
    }

    public static ColorClashCard skip(CardColor color) {
        return new ColorClashCard(color, CardType.SKIP, -1);
    }

    public static ColorClashCard reverse(CardColor color) {
        return new ColorClashCard(color, CardType.REVERSE, -1);
    }

    public static ColorClashCard drawTwo(CardColor color) {
        return new ColorClashCard(color, CardType.DRAW_TWO, -1);
    }

    public static ColorClashCard wild() {
        return new ColorClashCard(CardColor.WILD, CardType.WILD, -1);
    }

    public static ColorClashCard wildDrawFour() {
        return new ColorClashCard(CardColor.WILD, CardType.WILD_DRAW_FOUR, -1);
    }

    public boolean isWild() {
        return type == CardType.WILD || type == CardType.WILD_DRAW_FOUR;
    }

    public boolean isActionCard() {
        return type == CardType.SKIP || type == CardType.REVERSE || type == CardType.DRAW_TWO
                || type == CardType.WILD || type == CardType.WILD_DRAW_FOUR;
    }
}