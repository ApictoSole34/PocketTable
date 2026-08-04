package com.pockettable.server.service;

import com.pockettable.server.exception.RoomNotFoundException;
import com.pockettable.server.model.Room;
import com.pockettable.server.model.enums.RoomStatus;
import com.pockettable.server.repository.RoomRepository;
import com.pockettable.server.util.RoomCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomCodeGenerator roomCodeGenerator;


    public Room createRoom() {

        String roomCode = generateUniqueRoomCode();

        Room room = Room.builder()
                .roomCode(roomCode)
                .status(RoomStatus.WAITING)
                .build();

        return roomRepository.save(room);
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
}