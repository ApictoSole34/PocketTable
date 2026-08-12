package com.pockettable.server.service;

import com.pockettable.server.dto.room.CreateRoomRequest;
import com.pockettable.server.exception.InvalidRoomStateException;
import com.pockettable.server.exception.RoomNotFoundException;
import com.pockettable.server.model.Game;
import com.pockettable.server.model.Player;
import com.pockettable.server.model.Room;
import com.pockettable.server.model.enums.GameType;
import com.pockettable.server.model.enums.RoomStatus;
import com.pockettable.server.repository.GameRepository;
import com.pockettable.server.repository.PlayerRepository;
import com.pockettable.server.repository.RoomRepository;
import com.pockettable.server.service.game.engine.GameEngine;
import com.pockettable.server.service.game.engine.GameEngineFactory;
import com.pockettable.server.util.RoomCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomCodeGenerator roomCodeGenerator;
    private final PlayerRepository playerRepository;
    private final RoomEventService roomEventService;
    private final GameRepository gameRepository;
    private final GameEngineFactory gameEngineFactory;


    public Room createRoom(CreateRoomRequest request) {

        Room room = Room.builder()
                .roomCode(generateUniqueRoomCode())
                .status(RoomStatus.WAITING)
                .gameType(request.gameType())
                .maxPlayers(getDefaultMaxPlayers(request.gameType()))
                .build();

        return roomRepository.save(room);
    }

    private int getDefaultMaxPlayers(GameType gameType) {

        return switch (gameType) {

            case POKER -> 6;

            case UNO -> 10;

            case MAKAO -> 4;
        };
    }


    private String generateUniqueRoomCode() {

        String code;

        do {
            code = roomCodeGenerator.generate();
        } while (roomRepository.existsByRoomCode(code));

        return code;
    }

    public Room getRoomByCode(String roomCode) {

        return roomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new RoomNotFoundException(roomCode));
    }

    public List<Room> getAvaiableRooms() {

        return roomRepository.findByStatus(RoomStatus.WAITING);
    }

    public Room startGame(String roomCode, UUID playerId) {
        Room room = getRoomByCode(roomCode);

        if (gameRepository.findByRoomId(room.getId()).isPresent()) {

            throw new InvalidRoomStateException(
                    "Game for room " + roomCode + " already exists"
            );
        }

        if (room.getStatus() != RoomStatus.WAITING) {

            throw new InvalidRoomStateException(
                    "Room " + roomCode + " has already started"
            );
        }

        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new InvalidRoomStateException(
                        "Player " + playerId + " has not been found"
                )
        );

        if (!player.getRoom().getId().equals(room.getId())) {

            throw new InvalidRoomStateException(
                    "Player does not belong to this room"
            );
        }

        if (!player.isHost()) {

            throw new InvalidRoomStateException(
                    "Only the host can start the game"
            );
        }

        if (room.getPlayers().size() < 2) {

            throw new InvalidRoomStateException(
                    "At least 2 players are required"
            );
        }

        Game game = Game.builder()
                .room(room)
                .gameType(room.getGameType())
                .build();

        gameRepository.save(game);

        GameEngine engine = gameEngineFactory.getEngine(
                game.getGameType()
        );

        engine.start(game);

        room.setStatus(RoomStatus.PLAYING);

        Room savedRoom = roomRepository.save(room);

        roomEventService.gameStarted(roomCode);

        return savedRoom;
    }
}