package com.pockettable.server.dto.room;

import com.pockettable.server.dto.player.PlayerSummaryResponse;
import com.pockettable.server.model.enums.GameType;
import com.pockettable.server.model.enums.RoomStatus;

import java.util.List;
import java.util.UUID;

public record RoomResponse(
        UUID id,
        String roomCode,
        RoomStatus status,
        GameType gametype,
        int maxPlayers,
        List<PlayerSummaryResponse> players
) {
}