package com.fizzycoyote.pockettable.engine.colorclash;

import org.junit.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

public class ColorClashPlayerTest {

    private final UUID id = UUID.randomUUID();

    @Test
    public void constructor_setsPlayerIdAndName() {
        ColorClashPlayer player = new ColorClashPlayer(id);
        assertEquals(id, player.getPlayerId());
        assertTrue(player.getPlayerName().contains(id.toString()));
    }

    @Test
    public void setPlayerName_updatesName() {
        ColorClashPlayer player = new ColorClashPlayer(id);
        player.setPlayerName("Alice");
        assertEquals("Alice", player.getPlayerName());
    }

    @Test
    public void addCard_increasesHandSize() {
        ColorClashPlayer player = new ColorClashPlayer(id);
        ColorClashCard card = ColorClashCard.number(CardColor.RED, 5);
        player.addCard(card);
        assertEquals(1, player.getHandSize());
        assertEquals(1, player.getHand().size());
    }

    @Test
    public void addCards_addsMultiple() {
        ColorClashPlayer player = new ColorClashPlayer(id);
        player.addCards(List.of(
                ColorClashCard.number(CardColor.RED, 5),
                ColorClashCard.number(CardColor.BLUE, 3)
        ));
        assertEquals(2, player.getHandSize());
    }

    @Test
    public void removeCard_removesFromHand() {
        ColorClashPlayer player = new ColorClashPlayer(id);
        ColorClashCard card = ColorClashCard.number(CardColor.RED, 5);
        player.addCard(card);
        player.removeCard(card);
        assertEquals(0, player.getHandSize());
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeCard_notInHand_throwsException() {
        ColorClashPlayer player = new ColorClashPlayer(id);
        ColorClashCard card = ColorClashCard.number(CardColor.RED, 5);
        player.removeCard(card);
    }

    @Test
    public void hasWon_returnsTrueWhenHandEmpty() {
        ColorClashPlayer player = new ColorClashPlayer(id);
        assertTrue(player.hasWon());
        player.addCard(ColorClashCard.number(CardColor.RED, 5));
        assertFalse(player.hasWon());
    }

    @Test
    public void callLastCard_setsFlag() {
        ColorClashPlayer player = new ColorClashPlayer(id);
        player.addCard(ColorClashCard.number(CardColor.RED, 5));
        assertFalse(player.isCalledLastCard());
        player.callLastCard();
        assertTrue(player.isCalledLastCard());
    }

    @Test
    public void clearLastCardCall_resetsFlag() {
        ColorClashPlayer player = new ColorClashPlayer(id);
        player.addCard(ColorClashCard.number(CardColor.RED, 5));
        player.callLastCard();
        player.clearLastCardCall();
        assertFalse(player.isCalledLastCard());
    }

    @Test
    public void eliminate_setsEliminatedFlag() {
        ColorClashPlayer player = new ColorClashPlayer(id);
        assertFalse(player.isEliminated());
        player.eliminate();
        assertTrue(player.isEliminated());
    }

    @Test
    public void clearHand_removesAllCardsAndResetsLastCard() {
        ColorClashPlayer player = new ColorClashPlayer(id);
        player.addCard(ColorClashCard.number(CardColor.RED, 5));
        player.addCard(ColorClashCard.number(CardColor.BLUE, 3));
        player.callLastCard();
        player.clearHand();
        assertEquals(0, player.getHandSize());
        assertFalse(player.isCalledLastCard());
    }

    @Test
    public void addingCard_resetsLastCardCall() {
        ColorClashPlayer player = new ColorClashPlayer(id);
        player.addCard(ColorClashCard.number(CardColor.RED, 5));
        player.callLastCard();
        player.addCard(ColorClashCard.number(CardColor.BLUE, 3));
        assertFalse(player.isCalledLastCard());
    }

    @Test
    public void removeCard_whenHandSizeBecomesOne_doesNotAutoCallLastCard() {
        ColorClashPlayer player = new ColorClashPlayer(id);
        player.addCard(ColorClashCard.number(CardColor.RED, 5));
        player.addCard(ColorClashCard.number(CardColor.BLUE, 3));
        ColorClashCard card = player.getHand().get(0);
        player.removeCard(card);
        assertEquals(1, player.getHandSize());
        assertFalse(player.isCalledLastCard());
    }

    @Test
    public void getHand_returnsImmutableCopy() {
        ColorClashPlayer player = new ColorClashPlayer(id);
        player.addCard(ColorClashCard.number(CardColor.RED, 5));
        assertThrows(UnsupportedOperationException.class,
                () -> player.getHand().add(ColorClashCard.number(CardColor.BLUE, 3)));
    }
}