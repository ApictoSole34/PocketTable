package com.pockettable.server.service;

import com.pockettable.server.dto.player.JoinRoomRequest;
import com.pockettable.server.exception.DuplicateNicknameException;
import com.pockettable.server.exception.InvalidRoomStateException;
import com.pockettable.server.exception.RoomUnavailableException;
import com.pockettable.server.model.Player;
import com.pockettable.server.model.Room;
import com.pockettable.server.model.enums.RoomStatus;
import com.pockettable.server.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

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

        boolean firstPlayer = room.getPlayers().isEmpty();

        Player player = Player.builder()
                .nickname(request.nickname())
                .room(room)
                .host(firstPlayer)
                .build();


        Player saved = playerRepository.save(player);

        roomEventService.playerJoined(
                roomCode,
                saved.getNickname()
        );

        return saved;
    }

    public void leaveRoom(String roomCode, UUID playerId) {

        Room room = roomService.getRoomByCode(roomCode);

        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new InvalidRoomStateException(
                        "Player " + playerId + " is not found"
                ));

        if (!player.getRoom().getId().equals(room.getId())) {

            throw new InvalidRoomStateException(
                    "Player doest not belong to this room"
            );
        }

        String nickname = player.getNickname();
        boolean wasHost = player.isHost();

        room.getPlayers().remove(player);
        playerRepository.delete(player);

        if (wasHost) {
            room.getPlayers()
                    .stream()
                    .filter(p -> !p.getId().equals(playerId))
                    .findFirst()
                    .ifPresent(newHost -> {

                        newHost.setHost(true);
                        playerRepository.save(newHost);
                    });
        }


        roomEventService.playerLeft(
                roomCode,
                nickname
        );
    }
}