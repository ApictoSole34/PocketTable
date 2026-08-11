package com.pockettable.server.controller;

import com.pockettable.server.dto.player.PlayerSummaryResponse;
import com.pockettable.server.dto.room.CreateRoomRequest;
import com.pockettable.server.dto.room.RoomResponse;
import com.pockettable.server.model.Room;
import com.pockettable.server.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;


    @PostMapping
    public RoomResponse createRoom(
            @Valid @RequestBody CreateRoomRequest request
    ) {

        Room room = roomService.createRoom(request);

        return mapToResponse(room);
    }


    @GetMapping("/{roomCode}")
    public RoomResponse getRoom(
            @PathVariable String roomCode
    ) {

        Room room = roomService.getRoomByCode(roomCode);

        return mapToResponse(room);
    }

    @GetMapping
    public List<RoomResponse> getRooms() {

        return roomService.getAvaiableRooms()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private RoomResponse mapToResponse(Room room) {

        List<PlayerSummaryResponse> players =
                room.getPlayers()
                        .stream()
                        .map(player -> new PlayerSummaryResponse(
                                player.getId(),
                                player.getNickname(),
                                player.isHost()
                        ))
                        .toList();

        return new RoomResponse(
                room.getId(),
                room.getRoomCode(),
                room.getStatus(),
                room.getGameType(),
                room.getMaxPlayers(),
                players
        );
    }

    @PostMapping("/{roomCode}/start")
    public RoomResponse startRoom(
            @PathVariable String roomCode,
            @RequestParam UUID playerId
    ) {

        Room room = roomService.startGame(roomCode, playerId);
        return mapToResponse(room);
    }
}