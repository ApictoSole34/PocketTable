package com.pockettable.server.dto.player;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinRoomRequest(

        @NotBlank(message = "Nickname cannot be empty")
        @Size(
                min = 2,
                max = 20,
                message = "Nickname must have 2-20 characters"
        )
        String nickname

) {
}