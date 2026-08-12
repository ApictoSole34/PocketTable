package com.pockettable.server.service.game.poker;

import java.util.UUID;

public record PokerActionRequest(
        UUID playerId,
        PokerAction action,
        int amount
) {
}
