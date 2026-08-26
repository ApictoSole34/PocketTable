package com.fizzycoyote.pockettable.engine.colorclash;

public final class ColorClashRules {

    private ColorClashRules() {}

    public static boolean isPlayable(ColorClashCard card, ColorClashCard topCard,
                                     CardColor currentColor, int drawStack) {
        if (card == null || topCard == null) return false;

        if (drawStack > 0) return false;

        if (card.isWild()) return true;

        if (card.color() == currentColor) return true;

        if (card.type() == CardType.NUMBER && topCard.type() == CardType.NUMBER
                && card.value() == topCard.value()) {
            return true;
        }

        if (card.type() == CardType.SKIP && topCard.type() == CardType.SKIP) return true;
        if (card.type() == CardType.REVERSE && topCard.type() == CardType.REVERSE) return true;
        if (card.type() == CardType.DRAW_TWO && topCard.type() == CardType.DRAW_TWO) return true;

        return false;
    }
}