package com.fizzycoyote.pockettable.engine.poker;

import com.fizzycoyote.pockettable.engine.common.Card;
import com.fizzycoyote.pockettable.engine.common.CardRank;
import com.fizzycoyote.pockettable.engine.common.CardSuit;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PokerGameShowdownTest {

    private final UUID p0 = UUID.randomUUID();
    private final UUID p1 = UUID.randomUUID();
    private final UUID p2 = UUID.randomUUID();

    private Card c(CardSuit suit, CardRank rank) {
        return new Card(suit, rank);
    }


    @Test
    public void determineWinnerWithHand_picksPlayerWithBetterHand() {
        PokerGame game = new PokerGame("room", List.of(p0, p1));

        game.getPlayer(p0).addCard(c(CardSuit.HEARTS, CardRank.ACE));
        game.getPlayer(p0).addCard(c(CardSuit.DIAMONDS, CardRank.ACE));
        game.getPlayer(p1).addCard(c(CardSuit.CLUBS, CardRank.TWO));
        game.getPlayer(p1).addCard(c(CardSuit.SPADES, CardRank.SEVEN));

        game.setCommunityCardsForTest(List.of(
                c(CardSuit.HEARTS, CardRank.THREE),
                c(CardSuit.DIAMONDS, CardRank.NINE),
                c(CardSuit.CLUBS, CardRank.JACK),
                c(CardSuit.SPADES, CardRank.KING),
                c(CardSuit.HEARTS, CardRank.FOUR)
        ));

        PokerGame.WinnerResult result = game.determineWinnerWithHand(game.getPlayers());

        assertEquals(p0, result.player().getPlayerId());
        assertEquals(PokerHandRank.PAIR, result.hand().rank());
    }

    @Test
    public void threeHanded_twoFoldsLeaveOneWinnerWithoutShowdown() {
        PokerGame game = new PokerGame("room", List.of(p0, p1, p2));
        game.startGame(); // p1=SB, p2=BB, p0 na ruchu (button)

        game.performAction(p0, "FOLD", 0);
        game.performAction(p1, "FOLD", 0);

        assertTrue(game.isGameOver());
        assertEquals(p2, game.getWinnerId());
        assertEquals(1050, game.getPlayer(p2).getChips());
    }

    @Test
    public void allInWithDifferentStacks_shortStackWinsMainPotOnly() {
        PokerGame game = new PokerGame("room", List.of(p0, p1, p2));

        game.getPlayer(p0).addCard(c(CardSuit.HEARTS, CardRank.KING));
        game.getPlayer(p0).addCard(c(CardSuit.DIAMONDS, CardRank.KING));

        game.getPlayer(p1).addCard(c(CardSuit.CLUBS, CardRank.TWO));
        game.getPlayer(p1).addCard(c(CardSuit.SPADES, CardRank.THREE));

        game.getPlayer(p2).addCard(c(CardSuit.HEARTS, CardRank.QUEEN));
        game.getPlayer(p2).addCard(c(CardSuit.DIAMONDS, CardRank.QUEEN));

        game.setCommunityCardsForTest(List.of(
                c(CardSuit.CLUBS, CardRank.FOUR),
                c(CardSuit.SPADES, CardRank.SIX),
                c(CardSuit.HEARTS, CardRank.EIGHT),
                c(CardSuit.DIAMONDS, CardRank.TEN),
                c(CardSuit.CLUBS, CardRank.JACK)
        ));

        game.getPlayer(p0).addBet(50);
        game.getPlayer(p1).addBet(200);
        game.getPlayer(p2).addBet(200);

        List<SidePot> pots = game.buildSidePots();
        assertEquals(2, pots.size());

        SidePot mainPot = pots.get(0);
        assertEquals(150, mainPot.getAmount()); // 50 * 3
        PokerGame.WinnerResult mainWinner = game.determineWinnerWithHand(
                mainPot.getEligiblePlayerIds().stream().map(game::getPlayer).toList()
        );
        assertEquals(p0, mainWinner.player().getPlayerId());

        SidePot sidePot = pots.get(1);
        assertEquals(300, sidePot.getAmount()); // (200-50) * 2
        PokerGame.WinnerResult sideWinner = game.determineWinnerWithHand(
                sidePot.getEligiblePlayerIds().stream().map(game::getPlayer).toList()
        );
        assertEquals(p2, sideWinner.player().getPlayerId());
    }
}