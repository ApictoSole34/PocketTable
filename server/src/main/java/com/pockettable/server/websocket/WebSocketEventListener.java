package com.pockettable.server.websocket;

import com.pockettable.server.dto.game.poker.PokerGameState;
import com.pockettable.server.service.game.poker.PokerAction;
import com.pockettable.server.service.game.poker.PokerGame;
import com.pockettable.server.service.game.poker.PokerGameManager;
import com.pockettable.server.service.game.poker.PokerActionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final PokerGameManager gameManager;
    private final PokerActionService actionService;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String roomCode = (String) accessor.getSessionAttributes().get("roomCode");
        String playerIdStr = (String) accessor.getSessionAttributes().get("playerId");

        if (roomCode == null || playerIdStr == null) {
            log.warn("Brak roomCode lub playerId w sesji – pomijam automatyczny fold");
            return;
        }

        try {
            UUID playerId = UUID.fromString(playerIdStr);
            PokerGame game = gameManager.getGame(roomCode);

            if (game == null) {
                log.warn("Gra dla pokoju {} nie istnieje", roomCode);
                return;
            }

            if (!game.getCurrentPlayer().getPlayerId().equals(playerId)) {
                log.info("Gracz {} nie ma teraz tury – pomijam automatyczny fold", playerId);
                return;
            }

            if (game.getCurrentPlayer().isFolded() || game.getCurrentPlayer().isAllIn()) {
                log.info("Gracz {} jest już spasowany lub all-in", playerId);
                return;
            }

            actionService.performAction(game, playerId, PokerAction.FOLD, 0);

            PokerGameState state = PokerGameState.fromGame(game, null);
            messagingTemplate.convertAndSend("/topic/game/" + roomCode, state);

            log.info("Gracz {} został automatycznie spasowany po rozłączeniu", playerId);

        } catch (Exception e) {
            log.error("Błąd podczas automatycznego foldowania gracza: {}", e.getMessage());
        }
    }
}