package com.pockettable.server.service;

import com.pockettable.server.dto.room.CreateRoomRequest;
import com.pockettable.server.exception.RoomNotFoundException;
import com.pockettable.server.model.Room;
import com.pockettable.server.model.enums.GameType;
import com.pockettable.server.model.enums.RoomStatus;
import com.pockettable.server.repository.RoomRepository;
import com.pockettable.server.util.RoomCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomCodeGenerator roomCodeGenerator;


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
}