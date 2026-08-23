package com.fizzycoyote.pockettable.engine.poker;

import com.fizzycoyote.pockettable.engine.common.Card;
import com.fizzycoyote.pockettable.engine.common.CardRank;
import com.fizzycoyote.pockettable.engine.common.CardSuit;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class PokerPlayerTest {

    private final UUID id = UUID.randomUUID();

    @Test
    public void constructor_setsStartingChips() {
        PokerPlayer player = new PokerPlayer(id, 500);
        assertEquals(500, player.getChips());
    }

    @Test
    public void removeChips_subtractsExactAmount() {
        PokerPlayer player = new PokerPlayer(id, 1000);
        player.removeChips(300);
        assertEquals(700, player.getChips());
    }

    @Test
    public void removeChips_throwsWhenAmountExceedsChips() {
        PokerPlayer player = new PokerPlayer(id, 100);
        assertThrows(IllegalStateException.class, () -> player.removeChips(200));
    }

    @Test
    public void removeChips_throwsWhenAmountNotPositive() {
        PokerPlayer player = new PokerPlayer(id, 1000);
        assertThrows(IllegalArgumentException.class, () -> player.removeChips(0));
    }

    @Test
    public void removeChipsUpTo_removesFullAmountWhenEnoughChips() {
        PokerPlayer player = new PokerPlayer(id, 1000);
        int actual = player.removeChipsUpTo(300);

        assertEquals(300, actual);
        assertEquals(700, player.getChips());
        assertFalse(player.isAllIn());
    }

    @Test
    public void removeChipsUpTo_capsAtAvailableChipsAndSetsAllIn() {
        PokerPlayer player = new PokerPlayer(id, 150);
        int actual = player.removeChipsUpTo(300);

        assertEquals(150, actual);
        assertEquals(0, player.getChips());
        assertTrue(player.isAllIn());
    }

    @Test
    public void removeChipsUpTo_exactAmountAlsoTriggersAllIn() {
        PokerPlayer player = new PokerPlayer(id, 100);
        player.removeChipsUpTo(100);

        assertEquals(0, player.getChips());
        assertTrue(player.isAllIn());
    }

    @Test
    public void removeChipsUpTo_throwsWhenAmountNotPositive() {
        PokerPlayer player = new PokerPlayer(id, 1000);
        assertThrows(IllegalArgumentException.class, () -> player.removeChipsUpTo(-5));
    }

    @Test
    public void addChips_increasesChipsByAmount() {
        PokerPlayer player = new PokerPlayer(id, 1000);
        player.addChips(500);
        assertEquals(1500, player.getChips());
    }

    @Test
    public void addChips_throwsWhenAmountNotPositive() {
        PokerPlayer player = new PokerPlayer(id, 1000);
        assertThrows(IllegalArgumentException.class, () -> player.addChips(0));
    }

    @Test
    public void resetChips_setsExactAmount() {
        PokerPlayer player = new PokerPlayer(id, 1000);
        player.removeChips(400);
        player.resetChips(2000);
        assertEquals(2000, player.getChips());
    }

    @Test
    public void resetChips_throwsWhenAmountNegative() {
        PokerPlayer player = new PokerPlayer(id, 1000);
        assertThrows(IllegalArgumentException.class, () -> player.resetChips(-1));
    }

    @Test
    public void addBet_increasesCurrentBetAndTotalContribution() {
        PokerPlayer player = new PokerPlayer(id, 1000);
        player.addBet(100);
        player.addBet(50);

        assertEquals(150, player.getCurrentBet());
        assertEquals(150, player.getTotalContribution());
    }

    @Test
    public void addBet_throwsWhenAmountNotPositive() {
        PokerPlayer player = new PokerPlayer(id, 1000);
        assertThrows(IllegalArgumentException.class, () -> player.addBet(0));
    }

    @Test
    public void resetBet_clearsCurrentBetButKeepsTotalContribution() {
        PokerPlayer player = new PokerPlayer(id, 1000);
        player.addBet(200);
        player.resetBet();

        assertEquals(0, player.getCurrentBet());
        assertEquals(200, player.getTotalContribution());
    }

    @Test
    public void resetTotalContribution_clearsToZero() {
        PokerPlayer player = new PokerPlayer(id, 1000);
        player.addBet(200);
        player.resetTotalContribution();

        assertEquals(0, player.getTotalContribution());
    }

    @Test
    public void fold_setsFoldedTrue() {
        PokerPlayer player = new PokerPlayer(id, 1000);
        assertFalse(player.isFolded());

        player.fold();
        assertTrue(player.isFolded());
    }

    @Test
    public void resetFolded_clearsFoldedFlag() {
        PokerPlayer player = new PokerPlayer(id, 1000);
        player.fold();
        player.resetFolded();

        assertFalse(player.isFolded());
    }

    @Test
    public void resetAllIn_clearsAllInFlag() {
        PokerPlayer player = new PokerPlayer(id, 100);
        player.removeChipsUpTo(100);
        assertTrue(player.isAllIn());

        player.resetAllIn();
        assertFalse(player.isAllIn());
    }

    @Test
    public void markActed_and_resetActed_toggleActedFlag() {
        PokerPlayer player = new PokerPlayer(id, 1000);
        assertFalse(player.hasActed());

        player.markActed();
        assertTrue(player.hasActed());

        player.resetActed();
        assertFalse(player.hasActed());
    }

    @Test
    public void addCard_and_getHand_returnsAddedCards() {
        PokerPlayer player = new PokerPlayer(id, 1000);
        Card card1 = new Card(CardSuit.HEARTS, CardRank.ACE);
        Card card2 = new Card(CardSuit.SPADES, CardRank.KING);

        player.addCard(card1);
        player.addCard(card2);

        assertEquals(2, player.getHand().size());
        assertTrue(player.getHand().contains(card1));
        assertTrue(player.getHand().contains(card2));
    }

    @Test
    public void clearHand_removesAllCards() {
        PokerPlayer player = new PokerPlayer(id, 1000);
        player.addCard(new Card(CardSuit.HEARTS, CardRank.ACE));
        player.clearHand();

        assertTrue(player.getHand().isEmpty());
    }

    @Test
    public void getHand_returnsImmutableCopy() {
        PokerPlayer player = new PokerPlayer(id, 1000);
        player.addCard(new Card(CardSuit.HEARTS, CardRank.ACE));

        assertThrows(UnsupportedOperationException.class,
                () -> player.getHand().add(new Card(CardSuit.SPADES, CardRank.KING)));
    }

    @Test
    public void setPlayerName_updatesNameWhenNotNull() {
        PokerPlayer player = new PokerPlayer(id, 1000);
        player.setPlayerName("Adrian");
        assertEquals("Adrian", player.getPlayerName());
    }

    @Test
    public void setPlayerName_keepsOldNameWhenNullPassed() {
        PokerPlayer player = new PokerPlayer(id, 1000);
        player.setPlayerName("Adrian");
        player.setPlayerName(null);

        assertEquals("Adrian", player.getPlayerName());
    }
}