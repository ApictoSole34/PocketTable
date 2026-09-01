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

/**
 * Immutable snapshot of the current Color Clash game state.
 *
 * <p>This state is sent from the host to all connected clients after every action.
 * The viewer sees only their own hand; other players' hands are {@code null}.</p>
 *
 * @param players        map of player IDs to their public information ({@link PlayerInfo})
 * @param hands          map of player IDs to their hand cards – only the viewer's own hand is non-null
 * @param drawPileSize   number of cards remaining in the draw pile
 * @param topCard        the current top card of the discard pile
 * @param currentColor   the current active color (may differ from {@code topCard.color()} for wild cards)
 * @param currentPlayerId the UUID of the player whose turn it currently is
 * @param clockwise      {@code true} if turn order is clockwise, {@code false} for counter-clockwise
 * @param drawStack      number of cards the next player must draw (accumulated from DRAW_TWO and WILD_DRAW_FOUR)
 * @param winnerId       the UUID of the winning player, or {@code null} if the game is not over
 * @param viewerId       the UUID of the player who requested this state snapshot
 * @param rules          the active rule configuration ({@link ColorClashRules})
 *
 * @see #fromGame(ColorClashGame, UUID)
 */
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