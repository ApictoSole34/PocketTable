package com.pockettable.server.service.game.poker;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PokerActionService {

    private final PokerGameService gameService;

    public void performAction(
            PokerGame game,
            UUID playerId,
            PokerAction action,
            int amount
    ) {
        synchronized (game) {
            PokerPlayer player = game.getPlayer(playerId);

            if (game.getRound() == PokerRound.SHOWDOWN) {
                throw new IllegalStateException("Game is already in showdown");
            }

            if (!game.getCurrentPlayer().getPlayerId().equals(playerId)) {
                throw new IllegalStateException("It is not this player's turn");
            }

            if (player.isFolded()) {
                throw new IllegalStateException("Player has already folded");
            }

            if (player.isAllIn()) {
                throw new IllegalStateException("Player is already all-in and cannot act");
            }

            switch (action) {

                case FOLD -> {
                    player.fold();
                }

                case CHECK -> {
                    if (player.getCurrentBet() != game.getCurrentBet()) {
                        throw new IllegalStateException("Cannot check while current bet is higher");
                    }
                }

                case BET -> {
                    if (game.getCurrentBet() != 0) {
                        throw new IllegalStateException("Cannot bet when there is already a bet");
                    }
                    if (amount <= 0) {
                        throw new IllegalArgumentException("Bet amount must be positive");
                    }

                    int actual = player.removeChipsUpTo(amount);
                    player.addBet(actual);
                    game.setCurrentBet(player.getCurrentBet());
                    game.resetActedExcept(player);
                    game.addToPot(actual);
                }

                case CALL -> {
                    int amountToCall = game.getCurrentBet() - player.getCurrentBet();
                    if (amountToCall <= 0) {
                        throw new IllegalStateException("Nothing to call");
                    }

                    int actual = player.removeChipsUpTo(amountToCall);
                    player.addBet(actual);
                    game.addToPot(actual);

                }

                case RAISE -> {
                    if (amount <= game.getCurrentBet()) {
                        throw new IllegalArgumentException("Raise must be higher than current bet");
                    }

                    int amountToAdd = amount - player.getCurrentBet();
                    if (amountToAdd <= 0) {
                        throw new IllegalStateException("Player has already bet enough");
                    }

                    int actual = player.removeChipsUpTo(amountToAdd);
                    player.addBet(actual);
                    game.addToPot(actual);

                    boolean isFullRaise = player.getCurrentBet() > game.getCurrentBet();
                    if (isFullRaise) {
                        game.setCurrentBet(player.getCurrentBet());
                        game.resetActedExcept(player);
                    }
                }
            }

            player.markActed();

            long activePlayers = game.getPlayers()
                    .stream()
                    .filter(p -> !p.isFolded())
                    .count();

            if (activePlayers == 1) {
                gameService.finishGame(game);
                return;
            }

            if (game.allPlayersActed()) {
                advanceUntilActionOrShowdown(game);
            } else {
                game.nextPlayer();
            }
        }
    }

    private void advanceUntilActionOrShowdown(PokerGame game) {
        while (true) {
            game.advanceRound();

            if (game.getRound() == PokerRound.SHOWDOWN) {
                gameService.finishGame(game);
                return;
            }

            long playersWhoCanAct = game.getPlayers()
                    .stream()
                    .filter(p -> !p.isFolded() && !p.isAllIn())
                    .count();

            if (playersWhoCanAct >= 2) {
                return;
            }
        }
    }

    public void startNextHand(PokerGame game) {
        if (game.getRound() != PokerRound.SHOWDOWN) {
            throw new IllegalStateException("Current hand has not finished");
        }
        game.resetForNewHand();
    }
}