package com.pockettable.server.service.game;

import com.pockettable.server.model.Game;
import com.pockettable.server.model.enums.GameType;
import org.springframework.stereotype.Service;

@Service
public class UnoGameEngine implements GameEngine {

    @Override
    public GameType getGameType() {
        return GameType.UNO;
    }

    @Override
    public void start(Game game) {

        System.out.println(
                "Starting UNO game for room "
                        + game.getRoom().getRoomCode()
        );
    }
}
