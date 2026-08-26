package com.fizzycoyote.pockettable.engine.colorclash;

public class ColorClashRules {

    public static final ColorClashRules DEFAULT = new ColorClashRules(false, false, false, false);

    private final boolean stacking;
    private final boolean jumpIn;
    private final boolean sevenSwap;
    private final boolean zeroRotate;

    public ColorClashRules(boolean stacking, boolean jumpIn, boolean sevenSwap, boolean zeroRotate) {
        this.stacking = stacking;
        this.jumpIn = jumpIn;
        this.sevenSwap = sevenSwap;
        this.zeroRotate = zeroRotate;
    }

    public boolean stacking() { return stacking; }
    public boolean jumpIn() { return jumpIn; }
    public boolean sevenSwap() { return sevenSwap; }
    public boolean zeroRotate() { return zeroRotate; }

    /**
     * Sprawdza, czy karta może być zagrana na wierzch stosu.
     * Uwzględnia stacking – gdy drawStack > 0 i stacking=true, można zagrać tylko +2/+4.
     */
    public boolean canPlay(ColorClashCard card, ColorClashCard topCard,
                           CardColor currentColor, int drawStack) {
        if (card == null || topCard == null) return false;

        // Gdy gracz ma karę do dobrania
        if (drawStack > 0) {
            if (!stacking) return false;
            return card.type() == CardType.DRAW_TWO || card.type() == CardType.WILD_DRAW_FOUR;
        }

        // Karty dzikie zawsze można zagrać
        if (card.isWild()) return true;

        // Kolor pasuje
        if (card.color() == currentColor) return true;

        // Ta sama wartość (dla kart liczbowych)
        if (card.type() == CardType.NUMBER && topCard.type() == CardType.NUMBER
                && card.value() == topCard.value()) return true;

        // Ten sam typ akcji
        if (card.type() == CardType.SKIP && topCard.type() == CardType.SKIP) return true;
        if (card.type() == CardType.REVERSE && topCard.type() == CardType.REVERSE) return true;
        if (card.type() == CardType.DRAW_TWO && topCard.type() == CardType.DRAW_TWO) return true;

        return false;
    }

    @Override
    public String toString() {
        return "ColorClashRules{" +
                "stacking=" + stacking +
                ", jumpIn=" + jumpIn +
                ", sevenSwap=" + sevenSwap +
                ", zeroRotate=" + zeroRotate +
                '}';
    }
}