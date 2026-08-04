package com.pockettable.server.controller;

import com.pockettable.server.dto.player.JoinRoomRequest;
import com.pockettable.server.dto.player.PlayerResponse;
import com.pockettable.server.model.Player;
import com.pockettable.server.service.PlayerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerService playerService;


    @PostMapping("/{roomCode}/players")
    public PlayerResponse joinRoom(
            @PathVariable String roomCode,
            @Valid @RequestBody JoinRoomRequest request
    ) {

        Player player = playerService.joinRoom(roomCode, request);

        return new PlayerResponse(
                player.getId(),
                player.getNickname(),
                player.getRoom().getId()
        );
    }
}