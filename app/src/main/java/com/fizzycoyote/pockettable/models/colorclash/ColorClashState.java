package com.fizzycoyote.pockettable.models.colorclash;

import com.fizzycoyote.pockettable.engine.colorclash.CardColor;
import com.fizzycoyote.pockettable.engine.colorclash.ColorClashCard;
import com.fizzycoyote.pockettable.engine.colorclash.ColorClashGame;
import com.fizzycoyote.pockettable.engine.colorclash.ColorClashPlayer;
import com.fizzycoyote.pockettable.engine.colorclash.ColorClashRules;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public record ColorClashState(
        Map<UUID, PlayerInfo> players,
        Map<UUID, List<ColorClashCard>> hands,
        int drawPileSize,
        ColorClashCard topCard,
        CardColor currentColor,
        UUID currentPlayerId,
        boolean clockwise,
        int drawStack,
        UUID winnerId,
        UUID viewerId,
        ColorClashRules rules
) {
    public static ColorClashState fromGame(ColorClashGame game, UUID viewerId) {
        Map<UUID, PlayerInfo> playerInfos = game.getPlayers().stream()
                .collect(Collectors.toMap(
                        ColorClashPlayer::getPlayerId,
                        PlayerInfo::fromPlayer
                ));

        Map<UUID, List<ColorClashCard>> hands = new HashMap<>();
        for (ColorClashPlayer p : game.getPlayers()) {
            hands.put(p.getPlayerId(), p.getPlayerId().equals(viewerId) ? p.getHand() : null);
        }

        return new ColorClashState(
                playerInfos,
                hands,
                game.getDeck().size(),
                game.getTopCard(),
                game.getCurrentColor(),
                game.getCurrentPlayer() != null ? game.getCurrentPlayer().getPlayerId() : null,
                game.isClockwise(),
                game.getDrawStack(),
                game.getWinnerId(),
                viewerId,
                game.getRules()
        );
    }

    public record PlayerInfo(
            UUID playerId,
            String playerName,
            int handSize,
            boolean eliminated,
            boolean calledLastCard
    ) {
        public static PlayerInfo fromPlayer(ColorClashPlayer player) {
            return new PlayerInfo(
                    player.getPlayerId(),
                    player.getPlayerName(),
                    player.getHandSize(),
                    player.isEliminated(),
                    player.isCalledLastCard()
            );
        }
    }
}