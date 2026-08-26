package com.fizzycoyote.pockettable.game.colorclash;

import android.content.Context;

import com.fizzycoyote.pockettable.R;
import com.fizzycoyote.pockettable.engine.colorclash.CardColor;
import com.fizzycoyote.pockettable.engine.colorclash.ColorClashCard;

/**
 * Resolves the drawable resource for a Color Clash card.
 *
 * <p>Naming convention: {@code color_<color>_<value>} for number cards
 * (e.g. {@code color_blue_4}), {@code color_<color>_skip},
 * {@code color_<color>_reverse}, {@code color_<color>_drawtwo} for colored
 * action cards, and simply {@code wild} / {@code wild_drawfour} for the two
 * wild card types (their artwork doesn't depend on the color later chosen
 * for them).</p>
 */
public final class ColorClashCardResourceHelper {

    private ColorClashCardResourceHelper() {}

    public static int getCardResource(Context context, ColorClashCard card) {
        if (card == null) return R.drawable.card_back;

        String resourceName;
        switch (card.type()) {
            case WILD:
                if (card.color() == CardColor.WILD) {
                    resourceName = "wild";
                } else {
                    resourceName = "wild_" + card.color().name().toLowerCase();
                }
                break;
            case WILD_DRAW_FOUR:
                if (card.color() == CardColor.WILD) {
                    resourceName = "wild_drawfour";
                } else {
                    resourceName = "wild_drawfour_" + card.color().name().toLowerCase();
                }
                break;
            default:
                String color = card.color().name().toLowerCase();
                String suffix = switch (card.type()) {
                    case NUMBER -> String.valueOf(card.value());
                    case SKIP -> "skip";
                    case REVERSE -> "reverse";
                    case DRAW_TWO -> "drawtwo";
                    default -> "0";
                };
                resourceName = "color_" + color + "_" + suffix;
        }

        int resId = context.getResources().getIdentifier(resourceName, "drawable", context.getPackageName());
        return resId != 0 ? resId : R.drawable.card_back;
    }
}