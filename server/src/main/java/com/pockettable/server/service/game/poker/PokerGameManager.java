package com.pockettable.server.service.game.poker;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PokerGameManager {

    private final Map<String, PokerGame> games =
            new ConcurrentHashMap<>();

    public void addGame(String roomCode, PokerGame game) {
        games.put(roomCode, game);
    }

    public PokerGame getGame(String roomCode) {

        PokerGame game = games.get(roomCode);

        if (game == null) {
            throw new IllegalStateException(
                    "Poker game not found for room: " + roomCode
            );
        }

        return game;
    }

    public boolean hasGame(String roomCode) {
        return games.containsKey(roomCode);
    }

    public void removeGame(String roomCode) {
        games.remove(roomCode);
    }
}