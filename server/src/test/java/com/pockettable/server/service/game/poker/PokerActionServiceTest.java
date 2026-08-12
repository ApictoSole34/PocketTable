package com.pockettable.server.service.game.poker;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class PokerActionServiceTest {

    @Test
    void firstPlayerShouldBeCurrentPlayer() {

        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();

        PokerGame game = new PokerGame(
                List.of(player1, player2)
        );

        assertEquals(
                player1,
                game.getCurrentPlayer().getPlayerId()
        );
    }

    @Test
    void shouldMoveToNextPlayer() {

        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();

        PokerGame game = new PokerGame(
                List.of(player1, player2)
        );

        game.nextPlayer();

        assertEquals(
                player2,
                game.getCurrentPlayer().getPlayerId()
        );
    }

    @Test
    void shouldReturnToFirstPlayer() {

        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();

        PokerGame game = new PokerGame(
                List.of(player1, player2)
        );

        game.nextPlayer();
        game.nextPlayer();

        assertEquals(
                player1,
                game.getCurrentPlayer().getPlayerId()
        );
    }

    @Test
    void allPlayersMustAct() {

        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();

        PokerGame game = new PokerGame(
                List.of(player1, player2)
        );

        assertFalse(game.allPlayersActed());

        game.getPlayer(player1).markActed();

        assertFalse(game.allPlayersActed());

        game.getPlayer(player2).markActed();

        assertTrue(game.allPlayersActed());
    }

    @Test
    void foldedPlayerDoesNotNeedToAct() {

        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();

        PokerGame game = new PokerGame(
                List.of(player1, player2)
        );

        game.getPlayer(player1).markActed();
        game.getPlayer(player2).fold();

        assertTrue(game.allPlayersActed());
    }

    @Test
    void shouldAdvanceFromPreFlopToFlop() {

        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();

        PokerGame game = new PokerGame(
                List.of(player1, player2)
        );

        game.getPlayer(player1).markActed();
        game.getPlayer(player2).markActed();

        assertTrue(game.allPlayersActed());

        game.advanceRound();

        assertEquals(
                PokerRound.FLOP,
                game.getRound()
        );

        assertEquals(
                3,
                game.getCommunityCards().size()
        );

        assertFalse(game.getPlayer(player1).hasActed());
        assertFalse(game.getPlayer(player2).hasActed());
    }

    @Test
    void shouldAdvanceFromFlopToTurn() {

        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();

        PokerGame game = new PokerGame(
                List.of(player1, player2)
        );

        game.advanceRound();

        game.getPlayer(player1).markActed();
        game.getPlayer(player2).markActed();

        game.advanceRound();

        assertEquals(
                PokerRound.TURN,
                game.getRound()
        );

        assertEquals(
                4,
                game.getCommunityCards().size()
        );
    }

    @Test
    void shouldAdvanceFromTurnToRiver() {

        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();

        PokerGame game = new PokerGame(
                List.of(player1, player2)
        );

        game.advanceRound();
        game.advanceRound();

        game.getPlayer(player1).markActed();
        game.getPlayer(player2).markActed();

        game.advanceRound();

        assertEquals(
                PokerRound.RIVER,
                game.getRound()
        );

        assertEquals(
                5,
                game.getCommunityCards().size()
        );
    }

    @Test
    void shouldResetBetsWhenAdvancingRound() {

        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();

        PokerGame game = new PokerGame(
                List.of(player1, player2)
        );

        game.getPlayer(player1).addBet(100);
        game.getPlayer(player2).addBet(100);
        game.setCurrentBet(100);

        game.getPlayer(player1).markActed();
        game.getPlayer(player2).markActed();

        game.advanceRound();

        assertEquals(0, game.getCurrentBet());
        assertEquals(0, game.getPlayer(player1).getCurrentBet());
        assertEquals(0, game.getPlayer(player2).getCurrentBet());
    }

}
