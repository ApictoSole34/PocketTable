package com.pockettable.server.dto.room;

import com.pockettable.server.model.enums.RoomStatus;

import java.util.UUID;

public record RoomResponse(
        UUID id,
        String roomCode,
        RoomStatus status
) {
}