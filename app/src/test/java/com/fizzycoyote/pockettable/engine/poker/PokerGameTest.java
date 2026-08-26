package com.fizzycoyote.pockettable.engine.poker;

import org.junit.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class PokerGameTest {

    private final UUID p0 = UUID.randomUUID();
    private final UUID p1 = UUID.randomUUID();
    private final UUID p2 = UUID.randomUUID();

    @Test
    public void postBlinds_headsUp_dealerPostsSmallBlindAndActsFirst() {
        PokerGame game = new PokerGame("room", List.of(p0, p1));
        game.startGame();

        assertEquals(50, game.getPlayer(p0).getCurrentBet());
        assertEquals(100, game.getPlayer(p1).getCurrentBet());
        assertEquals(100, game.getCurrentBet());

        assertEquals(p0, game.getCurrentPlayer().getPlayerId());
    }

    @Test
    public void postBlinds_threeHanded_buttonActsFirstPreflop() {
        PokerGame game = new PokerGame("room", List.of(p0, p1, p2));
        game.startGame();

        assertEquals(50, game.getPlayer(p1).getCurrentBet());
        assertEquals(100, game.getPlayer(p2).getCurrentBet());
        assertEquals(0, game.getPlayer(p0).getCurrentBet());
        assertEquals(p0, game.getCurrentPlayer().getPlayerId());
    }

    @Test
    public void initialPot_equalsSumOfBlinds() {
        PokerGame game = new PokerGame("room", List.of(p0, p1));
        game.startGame();

        assertEquals(150, game.getTotalPot());
    }

    @Test
    public void performAction_throwsWhenNotPlayersTurn() {
        PokerGame game = new PokerGame("room", List.of(p0, p1));
        game.startGame();

        assertThrows(IllegalStateException.class,
                () -> game.performAction(p1, "CHECK", 0));
    }

    @Test
    public void performAction_throwsWhenGameAlreadyInShowdown() {
        PokerGame game = new PokerGame("room", List.of(p0, p1));
        game.advanceRound(); // PRE_FLOP -> FLOP
        game.advanceRound(); // FLOP -> TURN
        game.advanceRound(); // TURN -> RIVER
        game.advanceRound(); // RIVER -> SHOWDOWN

        assertThrows(IllegalStateException.class,
                () -> game.performAction(p0, "CHECK", 0));
    }

    @Test
    public void check_throwsWhenCurrentBetIsHigherThanPlayersBet() {
        PokerGame game = new PokerGame("room", List.of(p0, p1));
        game.startGame();

        assertThrows(IllegalStateException.class,
                () -> game.performAction(p0, "CHECK", 0));
    }

    @Test
    public void bet_throwsWhenThereIsAlreadyABet() {
        PokerGame game = new PokerGame("room", List.of(p0, p1));
        game.startGame();

        assertThrows(IllegalStateException.class,
                () -> game.performAction(p0, "BET", 200));
    }

    @Test
    public void raise_throwsWhenAmountNotHigherThanCurrentBet() {
        PokerGame game = new PokerGame("room", List.of(p0, p1));
        game.startGame();

        assertThrows(IllegalArgumentException.class,
                () -> game.performAction(p0, "RAISE", 100));
    }

    @Test
    public void call_throwsWhenNothingToCall() {
        PokerGame game = new PokerGame("room", List.of(p0, p1));
        game.startGame();
        game.performAction(p0, "CALL", 0);

        assertThrows(IllegalStateException.class,
                () -> game.performAction(p1, "CALL", 0));
    }

    @Test
    public void call_addsCorrectAmountToPotAndMatchesBet() {
        PokerGame game = new PokerGame("room", List.of(p0, p1));
        game.startGame();

        game.performAction(p0, "CALL", 0);

        assertEquals(900, game.getPlayer(p0).getChips());
        assertEquals(100, game.getPlayer(p0).getCurrentBet());
        assertEquals(200, game.getTotalPot());
    }

    @Test
    public void raise_updatesCurrentBetAndPotCorrectly() {
        PokerGame game = new PokerGame("room", List.of(p0, p1));
        game.startGame();

        game.performAction(p0, "RAISE", 300);

        assertEquals(300, game.getCurrentBet());
        assertEquals(300, game.getPlayer(p0).getCurrentBet());
        assertEquals(400, game.getTotalPot());
        assertEquals(p1, game.getCurrentPlayer().getPlayerId());
    }

    @Test
    public void fold_headsUp_remainingPlayerWinsImmediately() {
        PokerGame game = new PokerGame("room", List.of(p0, p1));
        game.startGame();

        game.performAction(p0, "FOLD", 0);

        assertTrue(game.isGameOver());
        assertEquals(p1, game.getWinnerId());
        assertEquals(1050, game.getPlayer(p1).getChips());
    }

    @Test
    public void bothPlayersCheckingMatchedBets_advancesToFlop() {
        PokerGame game = new PokerGame("room", List.of(p0, p1));
        game.startGame();

        game.performAction(p0, "CALL", 0);
        game.performAction(p1, "CHECK", 0);

        assertEquals(PokerRound.FLOP, game.getRound());
        assertEquals(200, game.getTotalPot());
        assertEquals(3, game.getCommunityCards().size());
    }

    @Test
    public void buildSidePots_equalContributions_createsSingleMainPot() {
        PokerGame game = new PokerGame("room", List.of(p0, p1));
        game.getPlayer(p0).addBet(100);
        game.getPlayer(p1).addBet(100);

        List<SidePot> pots = game.buildSidePots();

        assertEquals(1, pots.size());
        assertEquals(200, pots.get(0).getAmount());
        assertEquals(2, pots.get(0).getEligiblePlayerIds().size());
    }

    @Test
    public void buildSidePots_unequalAllIns_createsMainAndSidePot() {
        PokerGame game = new PokerGame("room", List.of(p0, p1, p2));
        game.getPlayer(p0).addBet(50);
        game.getPlayer(p1).addBet(150);
        game.getPlayer(p2).addBet(150);

        List<SidePot> pots = game.buildSidePots();

        assertEquals(2, pots.size());

        SidePot mainPot = pots.get(0);
        assertEquals(150, mainPot.getAmount());
        assertEquals(3, mainPot.getEligiblePlayerIds().size());

        SidePot sidePot = pots.get(1);
        assertEquals(200, sidePot.getAmount());
        assertEquals(2, sidePot.getEligiblePlayerIds().size());
        assertFalse(sidePot.getEligiblePlayerIds().contains(p0));
    }

    @Test
    public void buildSidePots_foldedPlayerMoneyGoesToFirstPot() {
        PokerGame game = new PokerGame("room", List.of(p0, p1, p2));
        game.getPlayer(p0).addBet(100);
        game.getPlayer(p0).fold();

        game.getPlayer(p1).addBet(200);
        game.getPlayer(p2).addBet(200);

        List<SidePot> pots = game.buildSidePots();

        assertEquals(1, pots.size());
        assertEquals(500, pots.get(0).getAmount());
        assertEquals(2, pots.get(0).getEligiblePlayerIds().size());
        assertFalse(pots.get(0).getEligiblePlayerIds().contains(p0));
    }

    @Test
    public void applySettings_updatesBlindsAndResetsChips() {
        PokerGame game = new PokerGame("room", List.of(p0, p1));

        game.applySettings(25, 50, 500);

        assertEquals(25, game.getSmallBlind());
        assertEquals(50, game.getBigBlind());
        assertEquals(500, game.getStartingChips());
        assertEquals(500, game.getPlayer(p0).getChips());
        assertEquals(500, game.getPlayer(p1).getChips());
    }

    @Test
    public void startGame_eliminatesPlayerBelowSmallBlindAndEndsTournament() {
        PokerGame game = new PokerGame("room", List.of(p0, p1));
        game.getPlayer(p1).removeChips(960);

        game.startGame();

        assertTrue(game.isTournamentOver());
        assertEquals(1, game.getPlayers().size());
        assertEquals(p0, game.getChampionId());
    }

    @Test
    public void resetForNewHand_movesDealerToNextPlayer() {
        PokerGame game = new PokerGame("room", List.of(p0, p1));
        game.startGame();

        UUID dealerBefore = game.getDealer().getPlayerId();
        game.resetForNewHand();
        UUID dealerAfter = game.getDealer().getPlayerId();

        assertNotNull(dealerAfter);
        assertFalse(dealerBefore.equals(dealerAfter));
    }
}