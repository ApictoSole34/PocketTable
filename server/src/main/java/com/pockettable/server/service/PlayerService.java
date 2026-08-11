package com.pockettable.server.service;

import com.pockettable.server.dto.player.JoinRoomRequest;
import com.pockettable.server.exception.DuplicateNicknameException;
import com.pockettable.server.exception.RoomUnavailableException;
import com.pockettable.server.model.Player;
import com.pockettable.server.model.Room;
import com.pockettable.server.model.enums.RoomStatus;
import com.pockettable.server.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final RoomService roomService;
    private final RoomEventService roomEventService;

    public Player joinRoom(String roomCode, JoinRoomRequest request) {

        Room room = roomService.getRoomByCode(roomCode);

        if(room.getStatus() != RoomStatus.WAITING) {

            throw new RoomUnavailableException(
              "Room" + roomCode + " is not accepting players"
            );
        }

        if(room.getPlayers().size() >= room.getMaxPlayers()) {

            throw new RoomUnavailableException(
                    "Room " + roomCode + " is full"
            );
        }

        if (playerRepository.existsByNicknameAndRoomId(
                request.nickname(),
                room.getId()
        )) {
            throw new DuplicateNicknameException(
                    request.nickname()
            );
        }


        Player player = Player.builder()
                .nickname(request.nickname())
                .room(room)
                .build();


        Player saved = playerRepository.save(player);

        roomEventService.playerJoined(
                roomCode,
                saved.getNickname()
        );

        return saved;
    }
}