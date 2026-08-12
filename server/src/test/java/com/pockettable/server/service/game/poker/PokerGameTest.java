package com.pockettable.server.service.game.poker;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PokerGameTest {

    @Test
    void gameAdvancesThroughRounds() {

        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();

        PokerGame game = new PokerGame(
                List.of(player1, player2)
        );

        assertEquals(PokerRound.PRE_FLOP, game.getRound());

        game.advanceRound();

        assertEquals(PokerRound.FLOP, game.getRound());
        assertEquals(3, game.getCommunityCards().size());

        game.advanceRound();

        assertEquals(PokerRound.TURN, game.getRound());
        assertEquals(4, game.getCommunityCards().size());

        game.advanceRound();

        assertEquals(PokerRound.RIVER, game.getRound());
        assertEquals(5, game.getCommunityCards().size());
    }

    @Test
    void cannotAdvanceAfterRiver() {
        PokerGame game = new PokerGame(
                List.of(UUID.randomUUID(), UUID.randomUUID())
        );

        game.advanceRound(); // PRE_FLOP -> FLOP
        game.advanceRound(); // FLOP -> TURN
        game.advanceRound(); // TURN -> RIVER
        game.advanceRound(); // RIVER -> SHOWDOWN (to już jest OK, nie rzuca)

        assertThrows(IllegalStateException.class, game::advanceRound); // SHOWDOWN -> wyjątek
    }

}
