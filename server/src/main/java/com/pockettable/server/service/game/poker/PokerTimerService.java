package com.pockettable.server.service.game.poker;

import com.pockettable.server.dto.game.poker.PokerGameState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PokerTimerService {

    private final PokerGameManager gameManager;
    private final PokerActionService actionService;
    private final SimpMessagingTemplate messagingTemplate;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private final Map<String, UUID> pendingActions = new ConcurrentHashMap<>();

    private int timeoutSeconds = 30;

    public void setTimeoutSeconds(int seconds) {
        this.timeoutSeconds = seconds;
    }

    public void startTimer(String roomCode, UUID playerId) {
        cancelTimer(roomCode);

        pendingActions.put(roomCode, playerId);

        scheduler.schedule(() -> {
            UUID expectedPlayerId = pendingActions.get(roomCode);
            if (expectedPlayerId == null || !expectedPlayerId.equals(playerId)) {
                return;
            }

            try {
                PokerGame game = gameManager.getGame(roomCode);
                if (game == null) {
                    log.warn("Gra dla pokoju {} nie istnieje – pomijam timeout", roomCode);
                    return;
                }

                if (game.getCurrentPlayer().getPlayerId().equals(playerId)
                        && !game.getCurrentPlayer().isFolded()
                        && !game.getCurrentPlayer().isAllIn()) {

                    actionService.performAction(game, playerId, PokerAction.FOLD, 0);
                    PokerGameState state = PokerGameState.fromGame(game, null); // null = broadcast
                    messagingTemplate.convertAndSend("/topic/game/" + roomCode, state);
                    log.info("Gracz {} został automatycznie spasowany po upływie czasu", playerId);
                }
            } catch (Exception e) {
                log.error("Błąd podczas automatycznego foldowania po timeout: {}", e.getMessage());
            } finally {
                pendingActions.remove(roomCode);
            }
        }, timeoutSeconds, TimeUnit.SECONDS);
    }

    public void cancelTimer(String roomCode) {
        pendingActions.remove(roomCode);
    }

    @Scheduled(fixedDelay = 60_000)
    public void cleanStaleTimers() {
        pendingActions.keySet().removeIf(roomCode -> {
            try {
                gameManager.getGame(roomCode);
                return false;
            } catch (Exception e) {
                return true;
            }
        });
    }
}