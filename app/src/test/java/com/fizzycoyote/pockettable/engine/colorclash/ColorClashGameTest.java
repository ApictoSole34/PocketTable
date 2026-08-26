package com.fizzycoyote.pockettable.engine.colorclash;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runners.MethodSorters;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

import com.fizzycoyote.pockettable.models.colorclash.ColorClashState;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ColorClashGameTest {

    private UUID p0, p1, p2;
    private ColorClashGame game;

    @Rule
    public Timeout globalTimeout = Timeout.seconds(10);

    @Before
    public void setUp() {
        p0 = UUID.randomUUID();
        p1 = UUID.randomUUID();
        p2 = UUID.randomUUID();
        game = new ColorClashGame(List.of(p0, p1, p2));
    }

    private void setTopCard(ColorClashCard card) throws Exception {
        Field field = ColorClashGame.class.getDeclaredField("topCard");
        field.setAccessible(true);
        field.set(game, card);
    }

    private void setCurrentColor(CardColor color) throws Exception {
        Field field = ColorClashGame.class.getDeclaredField("currentColor");
        field.setAccessible(true);
        field.set(game, color);
    }

    private void setCurrentPlayerIndex(int index) throws Exception {
        Field field = ColorClashGame.class.getDeclaredField("currentPlayerIndex");
        field.setAccessible(true);
        field.set(game, index);
    }

    private void setDrawStack(int stack) throws Exception {
        Field field = ColorClashGame.class.getDeclaredField("drawStack");
        field.setAccessible(true);
        field.set(game, stack);
    }

    private void setClockwise(boolean clockwise) throws Exception {
        Field field = ColorClashGame.class.getDeclaredField("clockwise");
        field.setAccessible(true);
        field.set(game, clockwise);
    }

    private void clearHand(UUID playerId) {
        game.getPlayer(playerId).clearHand();
    }

    private void addCardToHand(UUID playerId, ColorClashCard card) {
        game.getPlayer(playerId).addCard(card);
    }

    @Test
    public void game_initializesWithSevenCardsEach() {
        assertEquals(7, game.getPlayer(p0).getHandSize());
        assertEquals(7, game.getPlayer(p1).getHandSize());
        assertEquals(7, game.getPlayer(p2).getHandSize());
    }

    @Test
    public void game_initializesWithDiscardPileAndTopCard() {
        assertNotNull(game.getTopCard());
        assertFalse(game.getTopCard().isWild());
    }

    @Test
    public void game_initializesWithCurrentColorEqualToTopCardColor() {
        assertEquals(game.getTopCard().color(), game.getCurrentColor());
    }

    @Test
    public void nextPlayer_rotatesClockwise() throws Exception {
        setCurrentPlayerIndex(0);
        setClockwise(true);

        java.lang.reflect.Method nextPlayer = ColorClashGame.class.getDeclaredMethod("nextPlayer");
        nextPlayer.setAccessible(true);
        nextPlayer.invoke(game);

        assertEquals(p1, game.getCurrentPlayer().getPlayerId());

        nextPlayer.invoke(game);
        assertEquals(p2, game.getCurrentPlayer().getPlayerId());

        nextPlayer.invoke(game);
        assertEquals(p0, game.getCurrentPlayer().getPlayerId());
    }

    @Test
    public void nextPlayer_rotatesCounterClockwise() throws Exception {
        setCurrentPlayerIndex(0);
        setClockwise(false);

        java.lang.reflect.Method nextPlayer = ColorClashGame.class.getDeclaredMethod("nextPlayer");
        nextPlayer.setAccessible(true);
        nextPlayer.invoke(game);

        assertEquals(p2, game.getCurrentPlayer().getPlayerId());

        nextPlayer.invoke(game);
        assertEquals(p1, game.getCurrentPlayer().getPlayerId());

        nextPlayer.invoke(game);
        assertEquals(p0, game.getCurrentPlayer().getPlayerId());
    }

    @Test
    public void playNumberCard_matchingColor_works() throws Exception {
        game.setTopCardForTest(ColorClashCard.number(CardColor.RED, 5));
        game.setCurrentColorForTest(CardColor.RED);
        game.setCurrentPlayerIndexForTest(0);

        clearHand(p0);
        addCardToHand(p0, ColorClashCard.number(CardColor.RED, 3));
        addCardToHand(p0, ColorClashCard.number(CardColor.BLUE, 7));

        UUID before = game.getCurrentPlayer().getPlayerId();
        game.performAction(p0, "PLAY:0", 0);
        UUID after = game.getCurrentPlayer().getPlayerId();

        assertNotEquals(before, after);
        assertEquals(1, game.getPlayer(p0).getHandSize());
        assertEquals(CardColor.RED, game.getCurrentColor());
    }

    @Test
    public void playNumberCard_matchingValue_works() throws Exception {
        setTopCard(ColorClashCard.number(CardColor.RED, 5));
        setCurrentColor(CardColor.RED);
        setCurrentPlayerIndex(0);

        clearHand(p0);
        addCardToHand(p0, ColorClashCard.number(CardColor.BLUE, 5));
        addCardToHand(p0, ColorClashCard.number(CardColor.YELLOW, 9));

        UUID before = game.getCurrentPlayer().getPlayerId();
        game.performAction(p0, "PLAY:0", 0);
        UUID after = game.getCurrentPlayer().getPlayerId();

        assertNotEquals(before, after);
        assertEquals(1, game.getPlayer(p0).getHandSize());
        assertEquals(CardColor.BLUE, game.getCurrentColor());
    }

    @Test
    public void playSkipCard_skipsNextPlayer() throws Exception {
        setTopCard(ColorClashCard.skip(CardColor.RED));
        setCurrentColor(CardColor.RED);
        setCurrentPlayerIndex(0);

        clearHand(p0);
        addCardToHand(p0, ColorClashCard.skip(CardColor.RED));
        addCardToHand(p0, ColorClashCard.number(CardColor.RED, 1));

        game.performAction(p0, "PLAY:0", 0);
        assertEquals(p2, game.getCurrentPlayer().getPlayerId());
    }

    @Test
    public void playReverseCard_changesDirection() throws Exception {
        game.setTopCardForTest(ColorClashCard.reverse(CardColor.RED));
        game.setCurrentColorForTest(CardColor.RED);
        game.setCurrentPlayerIndexForTest(0);
        game.setClockwiseForTest(true);

        clearHand(p0);
        addCardToHand(p0, ColorClashCard.reverse(CardColor.RED));
        addCardToHand(p0, ColorClashCard.number(CardColor.RED, 4));

        game.performAction(p0, "PLAY:0", 0);

        assertFalse(game.isClockwise());
        assertEquals(p2, game.getCurrentPlayer().getPlayerId());
    }

    @Test
    public void playDrawTwo_addsToDrawStackAndSkipsTurn() throws Exception {
        setTopCard(ColorClashCard.drawTwo(CardColor.RED));
        setCurrentColor(CardColor.RED);
        setCurrentPlayerIndex(0);

        clearHand(p0);
        addCardToHand(p0, ColorClashCard.drawTwo(CardColor.RED));
        addCardToHand(p0, ColorClashCard.number(CardColor.RED, 2));

        game.performAction(p0, "PLAY:0", 0);

        assertEquals(2, game.getDrawStack());
        assertEquals(p1, game.getCurrentPlayer().getPlayerId());
    }

    @Test
    public void wildCard_changesCurrentColor() throws Exception {
        setTopCard(ColorClashCard.number(CardColor.RED, 5));
        setCurrentColor(CardColor.RED);
        setCurrentPlayerIndex(0);

        clearHand(p0);
        addCardToHand(p0, ColorClashCard.wild());
        addCardToHand(p0, ColorClashCard.number(CardColor.RED, 1));

        game.performAction(p0, "PLAY:0:BLUE", 0);
        assertEquals(CardColor.BLUE, game.getCurrentColor());
        assertEquals(CardColor.BLUE, game.getTopCard().color());
        assertTrue(game.getTopCard().isWild());
    }

    @Test
    public void wildDrawFour_addsFourToDrawStackAndSkipsTurn() throws Exception {
        setTopCard(ColorClashCard.number(CardColor.RED, 5));
        setCurrentColor(CardColor.RED);
        setCurrentPlayerIndex(0);

        clearHand(p0);
        addCardToHand(p0, ColorClashCard.wildDrawFour());
        addCardToHand(p0, ColorClashCard.number(CardColor.RED, 2));

        game.performAction(p0, "PLAY:0:BLUE", 0);

        assertEquals(4, game.getDrawStack());
        assertEquals(p1, game.getCurrentPlayer().getPlayerId());
        assertEquals(CardColor.BLUE, game.getCurrentColor());
    }

    @Test(expected = IllegalArgumentException.class)
    public void playWildWithoutChoosingColor_throwsException() throws Exception {
        setTopCard(ColorClashCard.number(CardColor.RED, 5));
        setCurrentColor(CardColor.RED);
        setCurrentPlayerIndex(0);

        clearHand(p0);
        addCardToHand(p0, ColorClashCard.wild());
        game.performAction(p0, "PLAY:0", 0);
    }

    @Test
    public void draw_whileDrawStackPositive_drawsStackCardsAndClearsStack() throws Exception {
        setDrawStack(3);
        setCurrentPlayerIndex(0);

        game.performAction(p0, "DRAW", 0);

        assertEquals(10, game.getPlayer(p0).getHandSize());
        assertEquals(0, game.getDrawStack());
        assertNotEquals(p0, game.getCurrentPlayer().getPlayerId());
    }

    @Test
    public void draw_whenDrawStackZero_drawsOneCard() throws Exception {
        setDrawStack(0);
        setCurrentPlayerIndex(0);

        int before = game.getPlayer(p0).getHandSize();
        game.performAction(p0, "DRAW", 0);

        assertEquals(before + 1, game.getPlayer(p0).getHandSize());
        assertNotEquals(p0, game.getCurrentPlayer().getPlayerId());
    }

    @Test
    public void cannotPlayAnyCardWhenDrawStackPositive() throws Exception {
        setDrawStack(2);
        setTopCard(ColorClashCard.number(CardColor.RED, 5));
        setCurrentColor(CardColor.RED);
        setCurrentPlayerIndex(0);

        clearHand(p0);
        addCardToHand(p0, ColorClashCard.number(CardColor.RED, 3));
        assertThrows(IllegalArgumentException.class, () ->
                game.performAction(p0, "PLAY:0", 0)
        );

        clearHand(p0);
        addCardToHand(p0, ColorClashCard.drawTwo(CardColor.RED));
        assertThrows(IllegalArgumentException.class, () ->
                game.performAction(p0, "PLAY:0", 0)
        );

        clearHand(p0);
        addCardToHand(p0, ColorClashCard.wildDrawFour());
        assertThrows(IllegalArgumentException.class, () ->
                game.performAction(p0, "PLAY:0:BLUE", 0)
        );
    }

    @Test
    public void cannotPlayDrawTwoWhenDrawStackPositive() throws Exception {
        setDrawStack(2);
        setTopCard(ColorClashCard.drawTwo(CardColor.RED));
        setCurrentColor(CardColor.RED);
        setCurrentPlayerIndex(0);

        clearHand(p0);
        addCardToHand(p0, ColorClashCard.drawTwo(CardColor.RED));

        assertThrows(IllegalArgumentException.class, () ->
                game.performAction(p0, "PLAY:0", 0)
        );
    }

    @Test
    public void cannotPlayWildDrawFourWhenDrawStackPositive() throws Exception {
        setDrawStack(2);
        setTopCard(ColorClashCard.number(CardColor.RED, 5));
        setCurrentColor(CardColor.RED);
        setCurrentPlayerIndex(0);

        clearHand(p0);
        addCardToHand(p0, ColorClashCard.wildDrawFour());

        assertThrows(IllegalArgumentException.class, () ->
                game.performAction(p0, "PLAY:0:BLUE", 0)
        );
    }

    @Test
    public void lastCardCall_whenOneCardLeft_marksPlayer() {
        ColorClashPlayer player = game.getPlayer(p0);
        clearHand(p0);
        addCardToHand(p0, ColorClashCard.number(CardColor.RED, 5));

        assertFalse(player.isCalledLastCard());
        game.performAction(p0, "CALL_LAST_CARD", 0);
        assertTrue(player.isCalledLastCard());
    }

    @Test(expected = IllegalArgumentException.class)
    public void lastCardCall_withMoreThanOneCard_throwsException() {
        ColorClashPlayer player = game.getPlayer(p0);
        clearHand(p0);
        addCardToHand(p0, ColorClashCard.number(CardColor.RED, 5));
        addCardToHand(p0, ColorClashCard.number(CardColor.BLUE, 3));

        game.performAction(p0, "CALL_LAST_CARD", 0);
    }

    @Test
    public void catch_opponentWithOneCard_addsTwoCardsToThem() throws Exception {
        ColorClashPlayer player1 = game.getPlayer(p1);
        clearHand(p1);
        addCardToHand(p1, ColorClashCard.number(CardColor.RED, 5));
        assertFalse(player1.isCalledLastCard());

        int before = player1.getHandSize();
        game.performAction(p0, "CATCH:" + p1, 0);

        assertEquals(before + 2, player1.getHandSize());
        assertTrue(player1.isCalledLastCard());
    }

    @Test(expected = IllegalArgumentException.class)
    public void catch_self_throwsException() {
        game.performAction(p0, "CATCH:" + p0, 0);
    }

    @Test
    public void catch_opponentWithMoreThanOneCard_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                game.performAction(p0, "CATCH:" + p1, 0)
        );
    }

    @Test
    public void catch_opponentAlreadyCalledLastCard_throwsException() {
        ColorClashPlayer player1 = game.getPlayer(p1);
        clearHand(p1);
        addCardToHand(p1, ColorClashCard.number(CardColor.RED, 5));
        player1.callLastCard();

        assertThrows(IllegalArgumentException.class, () ->
                game.performAction(p0, "CATCH:" + p1, 0)
        );
    }

    @Test
    public void resetForNewRound_reshufflesAndDealsSevenCards() throws Exception {
        ColorClashPlayer player = game.getPlayer(p0);
        int initialHandSize = player.getHandSize();

        game.resetForNewRound();

        assertEquals(7, game.getPlayer(p0).getHandSize());
        assertEquals(7, game.getPlayer(p1).getHandSize());
        assertEquals(7, game.getPlayer(p2).getHandSize());

        assertNotNull(game.getTopCard());
        assertFalse(game.getTopCard().isWild());
        assertEquals(0, game.getDrawStack());
        assertFalse(game.isGameOver());
        assertNull(game.getWinnerId());
    }

    @Test
    public void gameOverAfterPlayerWins() throws Exception {
        setTopCard(ColorClashCard.number(CardColor.RED, 5));
        setCurrentColor(CardColor.RED);
        setCurrentPlayerIndex(0);

        clearHand(p0);
        addCardToHand(p0, ColorClashCard.number(CardColor.RED, 3));

        game.performAction(p0, "PLAY:0", 0);

        assertTrue(game.isGameOver());
        assertEquals(p0, game.getWinnerId());
    }

    @Test
    public void dealCardsToPlayer_dealsCorrectNumberOfCards() {
        ColorClashPlayer player = game.getPlayer(p0);
        int before = player.getHandSize();
        game.dealCardsToPlayer(player, 3);
        assertEquals(before + 3, player.getHandSize());
    }

    @Test
    public void getPlayer_throwsIfNotFound() {
        UUID unknown = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> game.getPlayer(unknown));
    }

    @Test
    public void addPlayer_addsToGame() {
        UUID p3 = UUID.randomUUID();
        ColorClashPlayer newPlayer = new ColorClashPlayer(p3);
        game.addPlayer(newPlayer);
        assertEquals(newPlayer, game.getPlayer(p3));
        assertEquals(4, game.getPlayers().size());
    }

    @Test
    public void getState_returnsStateWithViewerHands() {
        ColorClashState state = (ColorClashState) game.getState(p0);

        assertNotNull(state.hands().get(p0));
        assertNull(state.hands().get(p1));
        assertNull(state.hands().get(p2));

        assertEquals(p0, state.viewerId());

        assertNotNull(state.players().get(p0));
        assertNotNull(state.players().get(p1));
        assertNotNull(state.players().get(p2));
    }

    @Test
    public void getState_returnsCurrentColorAndTopCard() {
        ColorClashState state = (ColorClashState) game.getState(p0);
        assertEquals(game.getTopCard(), state.topCard());
        assertEquals(game.getCurrentColor(), state.currentColor());
    }
}