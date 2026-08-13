package com.pockettable.server.dto.game.poker;


import com.pockettable.server.model.game.Card;
import com.pockettable.server.service.game.poker.PokerGame;
import com.pockettable.server.service.game.poker.PokerPlayer;
import com.pockettable.server.service.game.poker.PokerRound;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

public record PokerGameState(
        PokerRound round,
        int currentBet,
        int totalPot,
        List<Card> communityCards,
        Map<UUID, PlayerState> players,
        UUID currentPlayerId,
        UUID dealerId,
        UUID viewerId
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
                game.getCurrentPlayer().getPlayerId(),
                game.getDealer().getPlayerId(),
                viewerId
        );
    }

    public record PlayerState(
            UUID playerId,
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
                    player.getChips(),
                    player.getCurrentBet(),
                    player.isFolded(),
                    player.isAllIn(),
                    player.hasActed(),
                    showHand ? player.getHand() : null
            );
        }
    }

    private static class TestStompFrameHandler implements StompFrameHandler {
        private final BlockingQueue<PokerGameState> queue;

        public TestStompFrameHandler(BlockingQueue<PokerGameState> queue) {
            this.queue = queue;
        }

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return PokerGameState.class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            queue.offer((PokerGameState) payload);
        }
    }
}