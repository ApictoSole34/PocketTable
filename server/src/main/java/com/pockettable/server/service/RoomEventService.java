package com.pockettable.server.service;


import com.pockettable.server.dto.event.RoomEvent;
import com.pockettable.server.dto.event.RoomEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class RoomEventService {

    private final SimpMessagingTemplate messagingTemplate;


    public void playerJoined(
            String roomCode,
            String nickname
    ) {

        messagingTemplate.convertAndSend(
                "/topic/rooms/" + roomCode,
                new RoomEvent(
                        RoomEventType.PLAYER_JOINED,
                        roomCode,
                        nickname
                )
        );
    }

    public void playerLeft(
            String roomCode,
            String nickname
    ) {
        messagingTemplate.convertAndSend(
                "/topic/rooms" + roomCode,
                new RoomEvent(
                        RoomEventType.PLAYER_LEFT,
                        roomCode,
                        nickname
                )
        );
    }


    public void gameStarted(
            String roomCode
    ) {

        messagingTemplate.convertAndSend(
                "/topic/rooms/" + roomCode,
                new RoomEvent(
                        RoomEventType.GAME_STARTED,
                        roomCode,
                        null
                )
        );
    }
}