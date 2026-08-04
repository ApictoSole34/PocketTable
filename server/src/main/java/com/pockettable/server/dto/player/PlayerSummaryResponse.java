package com.pockettable.server.dto.player;

import java.util.UUID;

public record PlayerSummaryResponse(
        UUID id,
        String nickname
) {
}
