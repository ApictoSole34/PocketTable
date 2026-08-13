package com.pockettable.server.websocket.game.poker;

import com.pockettable.server.dto.game.poker.PokerGameState;
import com.pockettable.server.service.game.poker.PokerActionRequest;
import com.pockettable.server.service.game.poker.PokerGame;
import com.pockettable.server.service.game.poker.PokerGameManager;
import com.pockettable.server.service.game.poker.PokerActionService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class PokerWebSocketController {

    private final PokerGameManager gameManager;
    private final PokerActionService actionService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/game/{roomCode}/action")
    public void handleAction(@DestinationVariable String roomCode,
                             PokerActionRequest request) {
        PokerGame game = gameManager.getGame(roomCode);
        actionService.performAction(game, request.playerId(), request.action(), request.amount());
        PokerGameState state = PokerGameState.fromGame(game, null);
        messagingTemplate.convertAndSend("/topic/game/" + roomCode, state);
    }

    @MessageMapping("/game/{roomCode}/state")
    public void getState(@DestinationVariable String roomCode) {
        PokerGame game = gameManager.getGame(roomCode);
        PokerGameState state = PokerGameState.fromGame(game, null);
        messagingTemplate.convertAndSend("/topic/game/" + roomCode, state);
    }
}