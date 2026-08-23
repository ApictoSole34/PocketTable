package com.fizzycoyote.pockettable.models.poker;


import com.fizzycoyote.pockettable.engine.poker.PokerAction;

import java.util.UUID;

public record PokerActionRequest(
        UUID playerId,
        PokerAction action,
        int amount
) {
}
