package com.fizzycoyote.pockettable.models.poker;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;


import com.fizzycoyote.pockettable.engine.common.Card;
import com.fizzycoyote.pockettable.engine.poker.PokerGame;
import com.fizzycoyote.pockettable.engine.poker.PokerPlayer;
import com.fizzycoyote.pockettable.engine.poker.PokerRound;

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