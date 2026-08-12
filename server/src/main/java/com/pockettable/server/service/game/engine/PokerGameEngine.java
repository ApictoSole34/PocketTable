package com.pockettable.server.service.game.engine;

import com.pockettable.server.model.Game;
import com.pockettable.server.model.Player;
import com.pockettable.server.model.enums.GameType;
import com.pockettable.server.model.game.Card;
import com.pockettable.server.model.game.poker.PokerHand;
import com.pockettable.server.service.game.poker.PokerGame;
import com.pockettable.server.service.game.poker.PokerHandEvaluator;
import com.pockettable.server.service.game.poker.PokerPlayer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PokerGameEngine implements GameEngine{

    private final PokerHandEvaluator pokerHandEvaluator;

    @Override
    public GameType getGameType() {
        return GameType.POKER;
    }

    @Override
    public void start(Game game) {

        List<UUID> playerIds = game.getRoom()
                .getPlayers()
                .stream()
                .map(Player::getId)
                .toList();

        PokerGame pokerGame = new PokerGame(playerIds);

        pokerGame.dealInitialCards();
        pokerGame.dealFlop();
        pokerGame.dealTurn();
        pokerGame.dealRiver();

        System.out.println(
                "Starting Poker game for room "
                    + game.getRoom().getRoomCode()
        );

        for (PokerPlayer player : pokerGame.getPlayers()) {

            System.out.println(
                    player.getPlayerId()
                    + " -> "
                    + player.getHand()
            );
        }

        System.out.println(
                "Round: " + pokerGame.getRound()
        );

        System.out.println(
                "Community cards: " + pokerGame.getCommunityCards()
        );

        // -------------------------
        // TEST HAND EVALUATOR
        // -------------------------

        PokerPlayer player = pokerGame.getPlayers().get(0);
        List<Card> allCards = new ArrayList<>();

        allCards.addAll(player.getHand());
        allCards.addAll(pokerGame.getCommunityCards());

        System.out.println(
                "All cards: " + allCards
        );

        PokerHand hand = pokerHandEvaluator.evaluateBest(allCards);

        System.out.println(
                "Hand: " + hand.rank()
        );

        System.out.println(
                "Cards: " + hand.cards()
        );

    }
}
