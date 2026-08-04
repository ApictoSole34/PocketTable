package com.pockettable.server.controller;

import com.pockettable.server.dto.player.PlayerSummaryResponse;
import com.pockettable.server.dto.room.RoomResponse;
import com.pockettable.server.model.Room;
import com.pockettable.server.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;


    @PostMapping
    public RoomResponse createRoom() {

        Room room = roomService.createRoom();

        return new RoomResponse(
                room.getId(),
                room.getRoomCode(),
                room.getStatus(),
                List.of()
        );
    }


    @GetMapping("/{roomCode}")
    public RoomResponse getRoom(
            @PathVariable String roomCode
    ) {

        Room room = roomService.getRoomByCode(roomCode);

        List<PlayerSummaryResponse> players =
                room.getPlayers()
                        .stream()
                        .map(player -> new PlayerSummaryResponse(
                                player.getId(),
                                player.getNickname()
                        ))
                        .toList();


        return new RoomResponse(
                room.getId(),
                room.getRoomCode(),
                room.getStatus(),
                players
        );
    }
}