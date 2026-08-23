package com.fizzycoyote.pockettable.engine.common;

import java.util.UUID;

public interface GameEngine {
    void startGame();
    void performAction(UUID playerId, String action, int amount);
    Object getState(UUID viewerId);
    boolean isGameOver();
}