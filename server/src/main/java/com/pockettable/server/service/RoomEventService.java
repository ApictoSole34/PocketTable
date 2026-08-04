package com.pockettable.server.service;

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
                nickname + " joined"
        );
    }
}