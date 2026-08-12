package com.pockettable.server.dto.event;

public record RoomEvent(
        RoomEventType type,
        String roomCode,
        String nickname
) {
}
