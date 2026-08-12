package com.pockettable.server.service.game;

import com.pockettable.server.model.Room;
import com.pockettable.server.model.enums.GameType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GameEngineFactory {

    private final List<GameEngine> gameEngines;

    public GameEngine getEngine(GameType gameType) {

        return gameEngines.stream()
                .filter(engine -> engine.getGameType() == gameType)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No game engine found for " + gameType
                ));
    }
}
