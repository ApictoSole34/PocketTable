package com.fizzycoyote.pockettable.models.poker;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;


import com.fizzycoyote.pockettable.engine.common.Card;
import com.fizzycoyote.pockettable.engine.poker.PokerGame;
import com.fizzycoyote.pockettable.engine.poker.PokerPlayer;
import com.fizzycoyote.pockettable.engine.poker.PokerRound;

/**
 * Immutable snapshot of the current Poker game state.
 *
 * <p>This state is sent from the host to all connected clients after every action.
 * The viewer sees only their own hole cards; other players' hands are {@code null}.</p>
 *
 * @param round             current betting round ({@link PokerRound})
 * @param currentBet        the current bet amount that players must match to stay in the hand
 * @param totalPot          total chips in the pot
 * @param communityCards    the community cards on the table (size depends on the round)
 * @param players           map of player IDs to their state ({@link PlayerState})
 * @param currentPlayerId   the UUID of the player whose turn it currently is
 * @param dealerId          the UUID of the current dealer (button holder)
 * @param viewerId          the UUID of the player who requested this state snapshot
 * @param winnerId          the UUID of the winning player, or {@code null} if the hand is not over
 * @param winnerHandDesc    description of the winning hand (e.g., "FLUSH", "PAIR"), or {@code null}
 * @param turnamentOver     {@code true} if the tournament has ended (all but one player eliminated)
 * @param championId        the UUID of the tournament champion (only valid when {@code tournamentOver} is true)
 *
 * @see #fromGame(PokerGame, UUID)
 */
public record PokerGameState(
        PokerRound round,
        int currentBet,
        int totalPot,
        List<Card> communityCards,
        Map<UUID, PlayerState> players,
        UUID currentPlayerId,
        UUID dealerId,
        UUID viewerId,
        UUID winnerId,
        String winnerHandDesc,
        boolean turnamentOver,
        UUID championId
) {

    public static PokerGameState fromGame(PokerGame game, UUID viewerId) {
        return new PokerGameState(
                game.getRound(),
                game.getCurrentBet(),
                game.getTotalPot(),
                game.getCommunityCards(),
                game.getPlayers().stream()
                        .collect(Collectors.toMap(
                                PokerPlayer::getPlayerId,
                                p -> PlayerState.fromPlayer(p, p.getPlayerId().equals(viewerId))
                        )),
                game.getCurrentPlayer() != null ? game.getCurrentPlayer().getPlayerId() : null,
                game.getDealer() != null ? game.getDealer().getPlayerId() : null,
                viewerId,
                game.getWinnerId(),
                game.getWinnerHandDescription(),
                game.isTournamentOver(),
                game.getChampionId()
        );
    }

    public record PlayerState(
            UUID playerId,
            String playerName,
            int chips,
            int currentBet,
            boolean folded,
            boolean allIn,
            boolean hasActed,
            List<Card> hand
    ) {
        public static PlayerState fromPlayer(PokerPlayer player, boolean showHand) {
            return new PlayerState(
                    player.getPlayerId(),
                    player.getPlayerName(),
                    player.getChips(),
                    player.getCurrentBet(),
                    player.isFolded(),
                    player.isAllIn(),
                    player.hasActed(),
                    showHand ? player.getHand() : null
            );
        }
    }
}