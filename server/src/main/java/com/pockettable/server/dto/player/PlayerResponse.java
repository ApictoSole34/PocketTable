package com.pockettable.server.dto.player;

import java.util.UUID;

public record PlayerResponse(
        UUID id,
        String nickname,
        UUID roomId
) {
}