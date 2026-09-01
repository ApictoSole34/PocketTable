package com.fizzycoyote.pockettable.engine.colorclash;

import android.content.Context;

import androidx.annotation.VisibleForTesting;

import com.fizzycoyote.pockettable.R;
import com.fizzycoyote.pockettable.engine.common.GameEngine;
import com.fizzycoyote.pockettable.models.colorclash.ColorClashState;

import java.util.*;

public class ColorClashGame implements GameEngine {

    private final Context context;
    private final List<ColorClashPlayer> players;
    private final ColorClashDeck deck;
    private final List<ColorClashCard> discardPile = new ArrayList<>();
    private ColorClashCard topCard;
    private CardColor currentColor;
    private int currentPlayerIndex = 0;
    private boolean clockwise = true;
    private int drawStack = 0;
    private UUID winnerId = null;
    private boolean gameOver = false;
    private ColorClashRules rules;

    public ColorClashGame(Context context, List<UUID> playerIds) {
        this(context, playerIds, ColorClashRules.DEFAULT);
    }

    public ColorClashGame(List<UUID> playerIds) {
        this(null, playerIds, ColorClashRules.DEFAULT);
    }

    public ColorClashGame(Context context, List<UUID> playerIds, ColorClashRules rules) {
        this.context = (context != null) ? context.getApplicationContext() : null;
        this.players = new ArrayList<>();
        for (UUID id : playerIds) {
            this.players.add(new ColorClashPlayer(this.context, id));
        }
        this.rules = rules;
        this.deck = new ColorClashDeck();
        dealCards();
        startDiscardPile();
        currentColor = topCard.color();
    }

    public ColorClashGame(List<UUID> playerIds, ColorClashRules rules) {
        this(null, playerIds, rules);
    }

    public Context getContext() {
        return context;
    }

    public String getString(int resId, Object... args) {
        if (context != null) {
            return context.getString(resId, args);
        }
        return "?";
    }

    private void dealCards() {
        for (ColorClashPlayer player : players) {
            for (int i = 0; i < 7; i++) {
                player.addCard(deck.draw());
            }
        }
    }

    private void startDiscardPile() {
        topCard = deck.draw();
        while (topCard.isWild()) {
            deck.addCardToBottom(topCard);
            topCard = deck.draw();
        }
        discardPile.add(topCard);
        currentColor = topCard.color();
    }

    private void nextPlayer() {
        if (clockwise) {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        } else {
            currentPlayerIndex = (currentPlayerIndex - 1 + players.size()) % players.size();
        }
        int safety = 0;
        while (players.get(currentPlayerIndex).isEliminated() && safety < players.size()) {
            if (clockwise) {
                currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
            } else {
                currentPlayerIndex = (currentPlayerIndex - 1 + players.size()) % players.size();
            }
            safety++;
        }
    }

    private void skipPlayer() {
        nextPlayer();
        nextPlayer();
    }

    private void reverseDirection() {
        clockwise = !clockwise;
        if (players.size() == 2) {
            skipPlayer();
        } else {
            nextPlayer();
        }
    }

    private void applyDrawTwo() {
        drawStack += 2;
        nextPlayer();
    }

    private void applyWildDrawFour() {
        drawStack += 4;
        nextPlayer();
    }

    private void rotateAllHands() {
        if (players.size() < 2) return;
        List<List<ColorClashCard>> hands = new ArrayList<>();
        List<ColorClashPlayer> activePlayers = new ArrayList<>();
        for (ColorClashPlayer p : players) {
            if (!p.isEliminated()) {
                hands.add(new ArrayList<>(p.getHand()));
                activePlayers.add(p);
            }
        }
        if (hands.size() < 2) return;

        int n = activePlayers.size();
        if (clockwise) {
            for (int i = 0; i < n; i++) {
                activePlayers.get(i).clearHand();
                activePlayers.get(i).addCards(hands.get((i - 1 + n) % n));
            }
        } else {
            for (int i = 0; i < n; i++) {
                activePlayers.get(i).clearHand();
                activePlayers.get(i).addCards(hands.get((i + 1) % n));
            }
        }
    }

    private void swapHands(ColorClashPlayer p1, ColorClashPlayer p2) {
        List<ColorClashCard> temp = new ArrayList<>(p1.getHand());
        p1.clearHand();
        p1.addCards(new ArrayList<>(p2.getHand()));
        p2.clearHand();
        p2.addCards(temp);
    }

    public synchronized void dealCardsToPlayer(ColorClashPlayer player, int count) {
        for (int i = 0; i < count; i++) {
            if (deck.isEmpty()) refillDeckIfNeeded();
            player.addCard(deck.draw());
        }
    }

    private void refillDeckIfNeeded() {
        if (deck.isEmpty()) {
            ColorClashCard top = discardPile.remove(discardPile.size() - 1);
            deck.refillFrom(discardPile);
            discardPile.clear();
            discardPile.add(top);
            topCard = top;
        }
    }

    private void checkWinner(ColorClashPlayer player) {
        if (player.hasWon()) {
            winnerId = player.getPlayerId();
            gameOver = true;
        }
    }

    public synchronized void setRules(ColorClashRules rules) {
        this.rules = rules != null ? rules : ColorClashRules.DEFAULT;
    }

    public synchronized ColorClashRules getRules() {
        return rules;
    }

    @Override
    public synchronized void startGame() {}

    /**
     * Executes a player's action in the Color Clash game.
     *
     * <p>Supported action formats (the {@code action} parameter):
     * <table border="1" style="border-collapse: collapse; width: 100%;">
     *   <tr style="background: #f0f0f0;">
     *     <th style="padding: 8px;">Action</th>
     *     <th style="padding: 8px;">Format</th>
     *     <th style="padding: 8px;">Description</th>
     *   </tr>
     *   <tr>
     *     <td style="padding: 8px;"><b>PLAY</b></td>
     *     <td style="padding: 8px;"><code>PLAY:{index}</code></td>
     *     <td style="padding: 8px;">Plays the card at the given index in the player's hand.</td>
     *   </tr>
     *   <tr>
     *     <td style="padding: 8px;"><b>PLAY with color</b></td>
     *     <td style="padding: 8px;"><code>PLAY:{index}:{color}</code></td>
     *     <td style="padding: 8px;">Plays a wild card and chooses a color ({@code RED}, {@code YELLOW}, {@code GREEN}, {@code BLUE}).</td>
     *   </tr>
     *   <tr>
     *     <td style="padding: 8px;"><b>PLAY with target</b></td>
     *     <td style="padding: 8px;"><code>PLAY:{index}:TARGET:{uuid}</code></td>
     *     <td style="padding: 8px;">Plays a 7 card and swaps hands with the target player (requires {@code sevenSwap} rule).</td>
     *   </tr>
     *   <tr>
     *     <td style="padding: 8px;"><b>DRAW</b></td>
     *     <td style="padding: 8px;"><code>DRAW</code></td>
     *     <td style="padding: 8px;">Draws one card, or the entire draw stack if {@code drawStack > 0}.</td>
     *   </tr>
     *   <tr>
     *     <td style="padding: 8px;"><b>JUMP_IN</b></td>
     *     <td style="padding: 8px;"><code>JUMP_IN:{index}</code></td>
     *     <td style="padding: 8px;">Plays a card identical to the top card, outside your turn (requires {@code jumpIn} rule).</td>
     *   </tr>
     *   <tr>
     *     <td style="padding: 8px;"><b>CATCH</b></td>
     *     <td style="padding: 8px;"><code>CATCH:{uuid}</code></td>
     *     <td style="padding: 8px;">Catches an opponent who has exactly one card and hasn't called {@code CALL_LAST_CARD}.</td>
     *   </tr>
     *   <tr>
     *     <td style="padding: 8px;"><b>CALL_LAST_CARD</b></td>
     *     <td style="padding: 8px;"><code>CALL_LAST_CARD</code></td>
     *     <td style="padding: 8px;">Declares that the player has exactly one card left.</td>
     *   </tr>
     * </table>
     *
     * <p><b>Thread-safety:</b> This method is {@code synchronized} on the game instance
     * to prevent concurrent modifications from multiple threads.</p>
     *
     * @param playerId the UUID of the player performing the action
     * @param action   the action string in one of the formats described above
     * @param amount   numeric parameter (unused in Color Clash – always 0)
     * @throws IllegalStateException    if the game is already over, it's not the player's turn,
     *                                  or the player has been eliminated
     * @throws IllegalArgumentException if the action is unknown, the card index is out of bounds,
     *                                  the card cannot be played, or required parameters are missing
     */
    @Override
    public synchronized void performAction(UUID playerId, String action, int amount) {
        if (gameOver) {
            throw new IllegalStateException(getString(R.string.colorclash_error_game_over));
        }

        ColorClashPlayer player = getPlayerInternal(playerId);

        if (action.startsWith("JUMP_IN:")) {
            if (!rules.jumpIn()) {
                throw new IllegalStateException(getString(R.string.colorclash_error_jumpin_disabled));
            }
            if (drawStack > 0) {
                throw new IllegalStateException(getString(R.string.colorclash_error_jumpin_draw_active));
            }
            if (getCurrentPlayerInternal().getPlayerId().equals(playerId)) {
                throw new IllegalStateException(getString(R.string.colorclash_error_use_play));
            }
            int index = Integer.parseInt(action.substring("JUMP_IN:".length()));
            if (index < 0 || index >= player.getHand().size()) {
                throw new IllegalArgumentException(getString(R.string.colorclash_error_invalid_card_index));
            }
            ColorClashCard card = player.getHand().get(index);
            if (!card.equals(topCard)) {
                throw new IllegalArgumentException(getString(R.string.colorclash_error_jumpin_requires_match));
            }
            player.removeCard(card);
            discardPile.add(card);
            topCard = card;
            if (!card.isWild()) currentColor = card.color();
            checkWinner(player);
            if (gameOver) return;

            currentPlayerIndex = players.indexOf(player);
            refillDeckIfNeeded();
            return;
        }

        if (action.equals("CALL_LAST_CARD")) {
            if (player.getHandSize() != 1) {
                throw new IllegalArgumentException(getString(R.string.colorclash_error_last_card_single_only));
            }
            player.callLastCard();
            return;
        }

        if (action.startsWith("CATCH:")) {
            UUID targetId = UUID.fromString(action.substring("CATCH:".length()));
            if (targetId.equals(playerId)) {
                throw new IllegalArgumentException(getString(R.string.colorclash_error_cannot_catch_self));
            }
            ColorClashPlayer target = getPlayerInternal(targetId);
            if (target.getHandSize() != 1 || target.isCalledLastCard()) {
                throw new IllegalArgumentException(
                        getString(R.string.colorclash_error_nothing_to_catch, target.getPlayerName())
                );
            }
            dealCardsToPlayerInternal(target, 2);
            target.callLastCard();
            return;
        }

        if (!getCurrentPlayerInternal().getPlayerId().equals(playerId)) {
            throw new IllegalStateException(getString(R.string.colorclash_error_not_your_turn));
        }
        if (player.isEliminated()) {
            throw new IllegalStateException(getString(R.string.colorclash_error_player_eliminated));
        }

        if (action.startsWith("PLAY:")) {
            String[] parts = action.split(":");
            int cardIndex = Integer.parseInt(parts[1]);
            CardColor chosenColor = null;
            UUID swapTarget = null;

            if (parts.length > 2) {
                if ("TARGET".equals(parts[2]) && parts.length > 3) {
                    swapTarget = UUID.fromString(parts[3]);
                } else {
                    chosenColor = CardColor.valueOf(parts[2]);
                }
            }

            if (cardIndex < 0 || cardIndex >= player.getHand().size()) {
                throw new IllegalArgumentException(getString(R.string.colorclash_error_invalid_card_index));
            }

            ColorClashCard card = player.getHand().get(cardIndex);

            if (!rules.canPlay(card, topCard, currentColor, drawStack)) {
                throw new IllegalArgumentException(getString(R.string.colorclash_error_card_cannot_be_played));
            }

            if (card.isWild() && chosenColor == null) {
                throw new IllegalArgumentException(getString(R.string.colorclash_error_choose_wild_color));
            }

            boolean isSeven = card.type() == CardType.NUMBER && card.value() == 7;
            if (isSeven && rules.sevenSwap() && player.getHandSize() > 1 && swapTarget == null) {
                throw new IllegalArgumentException(getString(R.string.colorclash_error_choose_swap_target));
            }

            player.removeCard(card);
            discardPile.add(card);
            topCard = card;

            if (card.isWild() && chosenColor != null) {
                topCard = new ColorClashCard(chosenColor, card.type(), card.value());
                discardPile.remove(discardPile.size() - 1);
                discardPile.add(topCard);
                currentColor = chosenColor;
            } else if (!card.isWild()) {
                currentColor = card.color();
            } else {
                currentColor = card.color();
            }

            if (isSeven && rules.sevenSwap() && swapTarget != null && !player.hasWon()) {
                ColorClashPlayer target = getPlayerInternal(swapTarget);
                swapHands(player, target);
            }

            checkWinner(player);
            if (gameOver) return;

            boolean isZero = card.type() == CardType.NUMBER && card.value() == 0;
            if (isZero && rules.zeroRotate() && !gameOver) {
                rotateAllHands();
            }

            if (drawStack == 0) {
                switch (card.type()) {
                    case SKIP -> skipPlayer();
                    case REVERSE -> reverseDirection();
                    case DRAW_TWO -> applyDrawTwo();
                    case WILD_DRAW_FOUR -> applyWildDrawFour();
                    default -> nextPlayer();
                }
            } else {
                if (card.type() == CardType.DRAW_TWO || card.type() == CardType.WILD_DRAW_FOUR) {
                    int additional = card.type() == CardType.WILD_DRAW_FOUR ? 4 : 2;
                    drawStack += additional;
                }
                nextPlayer();
            }

            refillDeckIfNeeded();

        } else if (action.equals("DRAW")) {
            refillDeckIfNeeded();

            int toDraw = drawStack > 0 ? drawStack : 1;
            for (int i = 0; i < toDraw; i++) {
                if (deck.isEmpty()) refillDeckIfNeeded();
                player.addCard(deck.draw());
            }
            drawStack = 0;
            nextPlayer();

        } else if (action.startsWith("CHOOSE_COLOR:")) {
            String colorStr = action.split(":")[1];
            CardColor chosenColor = CardColor.valueOf(colorStr);
            currentColor = chosenColor;

        } else {
            throw new IllegalArgumentException(getString(R.string.colorclash_error_unknown_action, action));
        }
    }

    @Override
    public synchronized Object getState(UUID viewerId) {
        return ColorClashState.fromGame(this, viewerId);
    }

    @Override
    public synchronized boolean isGameOver() {
        return gameOver;
    }

    public synchronized void resetForNewRound() {
        gameOver = false;
        winnerId = null;
        drawStack = 0;
        currentPlayerIndex = 0;
        clockwise = true;

        for (ColorClashPlayer p : players) {
            p.clearHand();
        }
        discardPile.clear();
        deck.reset();

        for (ColorClashPlayer p : players) {
            for (int i = 0; i < 7; i++) {
                p.addCard(deck.draw());
            }
        }

        startDiscardPile();
        currentColor = topCard.color();
    }

    public synchronized List<ColorClashPlayer> getPlayers() {
        return new ArrayList<>(players);
    }

    public synchronized ColorClashPlayer getPlayer(UUID playerId) {
        return getPlayerInternal(playerId);
    }

    private ColorClashPlayer getPlayerInternal(UUID playerId) {
        for (ColorClashPlayer p : players) {
            if (p.getPlayerId().equals(playerId)) return p;
        }
        throw new IllegalArgumentException(getString(R.string.colorclash_error_player_not_found, playerId));
    }

    public synchronized ColorClashPlayer getCurrentPlayer() {
        return getCurrentPlayerInternal();
    }

    private ColorClashPlayer getCurrentPlayerInternal() {
        if (players.isEmpty()) return null;
        return players.get(currentPlayerIndex);
    }

    public synchronized ColorClashCard getTopCard() {
        return topCard;
    }

    public synchronized CardColor getCurrentColor() {
        return currentColor;
    }

    public synchronized int getDrawStack() {
        return drawStack;
    }

    public synchronized boolean isClockwise() {
        return clockwise;
    }

    public synchronized ColorClashDeck getDeck() {
        return deck;
    }

    public synchronized UUID getWinnerId() {
        return winnerId;
    }

    public synchronized void addPlayer(ColorClashPlayer player) {
        players.add(player);
    }

    public synchronized boolean hasCalledLastCard(UUID playerId) {
        return getPlayerInternal(playerId).isCalledLastCard();
    }

    private void dealCardsToPlayerInternal(ColorClashPlayer player, int count) {
        for (int i = 0; i < count; i++) {
            if (deck.isEmpty()) refillDeckIfNeeded();
            player.addCard(deck.draw());
        }
    }

    // ============= for testing ========================
    @VisibleForTesting
    void setTopCardForTest(ColorClashCard card) { this.topCard = card; }

    @VisibleForTesting
    void setCurrentColorForTest(CardColor color) { this.currentColor = color; }

    @VisibleForTesting
    void setCurrentPlayerIndexForTest(int index) { this.currentPlayerIndex = index; }

    @VisibleForTesting
    void setDrawStackForTest(int stack) { this.drawStack = stack; }

    @VisibleForTesting
    void setClockwiseForTest(boolean clockwise) { this.clockwise = clockwise; }

    @VisibleForTesting
    void setGameOverForTest(boolean gameOver) { this.gameOver = gameOver; }

    @VisibleForTesting
    void setWinnerIdForTest(UUID winnerId) { this.winnerId = winnerId; }
}