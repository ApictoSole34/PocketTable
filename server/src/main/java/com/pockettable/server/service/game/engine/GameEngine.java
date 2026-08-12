package com.pockettable.server.service.game.engine;

import com.pockettable.server.model.Game;
import com.pockettable.server.model.enums.GameType;

public interface GameEngine {

    GameType getGameType();

    void start(Game game);
}
