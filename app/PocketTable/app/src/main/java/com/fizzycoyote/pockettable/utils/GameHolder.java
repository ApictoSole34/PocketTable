package com.fizzycoyote.pockettable.utils;

import com.fizzycoyote.pockettable.engine.poker.PokerGame;
import com.fizzycoyote.pockettable.network.PokerHostServer;

public class GameHolder {
    private static GameHolder instance;
    private PokerGame game;
    private PokerHostServer server;

    private GameHolder() {}

    public static GameHolder getInstance() {
        if (instance == null) instance = new GameHolder();
        return instance;
    }

    public void setGame(PokerGame game, PokerHostServer server) {
        this.game = game;
        this.server = server;
    }

    public PokerGame getGame() {
        return game;
    }

    public PokerHostServer getServer() {
        return server;
    }

    public void clear() {
        game = null;
        server = null;
    }
}