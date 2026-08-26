package com.fizzycoyote.pockettable.utils;

import com.fizzycoyote.pockettable.engine.common.GameEngine;
import com.fizzycoyote.pockettable.network.common.GenericHostServer;

public class GameHolder {
    private static GameHolder instance;
    private GameEngine game;
    private GenericHostServer server;

    private GameHolder() {}

    public static GameHolder getInstance() {
        if (instance == null) instance = new GameHolder();
        return instance;
    }

    public void setGame(GameEngine game, GenericHostServer server) {
        this.game = game;
        this.server = server;
    }

    public GameEngine getGame() {
        return game;
    }

    public GenericHostServer getServer() {
        return server;
    }

    public void clear() {
        game = null;
        server = null;
    }
}
