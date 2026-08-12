package com.pockettable.server.service.game.poker;

import com.pockettable.server.model.Player;
import com.pockettable.server.model.Room;
import com.pockettable.server.model.enums.GameType;
import com.pockettable.server.model.game.Card;
import com.pockettable.server.model.game.poker.PokerHand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PokerGameService {

    private final PokerHandEvaluator handEvaluator;
    private final PokerGameManager gameManager;

    public PokerGame startGame(Room room) {

        if (room.getGameType() != GameType.POKER) {
            throw new IllegalStateException(
                    "This room is not a poker room"
            );
        }

        if (gameManager.hasGame(room.getRoomCode())) {
            throw new IllegalStateException(
                    "Poker game has already started"
            );
        }

        List<UUID> playerIds = room.getPlayers()
                .stream()
                .map(Player::getId)
                .toList();

        PokerGame game = new PokerGame(playerIds);

        game.startNewHand();

        gameManager.addGame(
                room.getRoomCode(),
                game
        );

        return game;
    }

    public PokerPlayer determineWinner(PokerGame game, List<PokerPlayer> eligiblePlayers) {
        PokerPlayer winner = null;
        PokerHand bestHand = null;

        for (PokerPlayer player : eligiblePlayers) {
            if (player.isFolded()) {
                continue;
            }

            List<Card> cards = new ArrayList<>(player.getHand());
            cards.addAll(game.getCommunityCards());

            PokerHand hand = handEvaluator.evaluateBest(cards);

            if (bestHand == null || hand.compareTo(bestHand) > 0) {
                bestHand = hand;
                winner = player;
            }
        }

        if (winner == null) {
            throw new IllegalStateException("No eligible active players");
        }

        return winner;
    }

    public PokerPlayer getWinnerByFold(PokerGame game) {

        List<PokerPlayer> activePlayers = game.getPlayers()
                .stream()
                .filter(player -> !player.isFolded())
                .toList();

        if (activePlayers.size() != 1) {
            throw new IllegalStateException(
                    "There is not exactly one active player"
            );
        }

        return activePlayers.getFirst();
    }

    public PokerPlayer finishGame(PokerGame game) {
        List<PokerPlayer> activePlayers = game.getPlayers()
                .stream()
                .filter(p -> !p.isFolded())
                .toList();

        if (activePlayers.isEmpty()) {
            throw new IllegalStateException("No active players");
        }

        if (activePlayers.size() == 1) {
            PokerPlayer winner = activePlayers.getFirst();
            winner.addChips(game.getTotalPot());
            game.resetForNewHand();
            return winner;
        }

        if (game.getRound() != PokerRound.SHOWDOWN) {
            throw new IllegalStateException("Game has not reached showdown");
        }

        List<SidePot> sidePots = game.buildSidePots();
        if (sidePots.isEmpty()) {
            throw new IllegalStateException("No pots to award");
        }

        PokerPlayer mainWinner = null;

        for (SidePot pot : sidePots) {
            List<PokerPlayer> eligible = pot.getEligiblePlayerIds().stream()
                    .map(game::getPlayer)
                    .toList();

            PokerPlayer winner = determineWinner(game, eligible);
            winner.addChips(pot.getAmount());

            if (mainWinner == null) {
                mainWinner = winner;
            }
        }

        game.resetForNewHand();
        return mainWinner;
    }

}