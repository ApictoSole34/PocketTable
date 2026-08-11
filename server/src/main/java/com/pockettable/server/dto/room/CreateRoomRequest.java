package com.pockettable.server.dto.room;

import com.pockettable.server.model.enums.GameType;
import jakarta.validation.constraints.NotNull;

public record CreateRoomRequest(

        @NotNull(message = "Game type is required")
        GameType gameType

) {
}