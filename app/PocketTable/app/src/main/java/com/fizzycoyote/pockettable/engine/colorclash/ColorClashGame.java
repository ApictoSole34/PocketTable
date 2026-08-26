package com.fizzycoyote.pockettable.engine.colorclash;


import com.fizzycoyote.pockettable.engine.common.GameEngine;
import com.fizzycoyote.pockettable.models.colorclash.ColorClashState;

import java.util.*;

public class ColorClashGame implements GameEngine {

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

    public ColorClashGame(List<UUID> playerIds) {
        this.players = new ArrayList<>();
        for (UUID id : playerIds) {
            ColorClashPlayer player = new ColorClashPlayer(id);
            this.players.add(player);
        }
        this.deck = new ColorClashDeck();
        dealCards();
        startDiscardPile();
        currentColor = topCard.color();
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
            deck.addCard(topCard);
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
        while (players.get(currentPlayerIndex).isEliminated()) {
            if (clockwise) {
                currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
            } else {
                currentPlayerIndex = (currentPlayerIndex - 1 + players.size()) % players.size();
            }
        }
    }

    private void skipPlayer() {
        nextPlayer();
        nextPlayer();
    }

    private void reverseDirection() {
        clockwise = !clockwise;
        nextPlayer();
    }

    private void applyDrawTwo() {
        drawStack += 2;
        nextPlayer();
    }

    private void applyWildDrawFour() {
        drawStack += 4;
        nextPlayer();
    }

    public void dealCardsToPlayer(ColorClashPlayer player, int count) {
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

    @Override
    public void startGame() {}

    @Override
    public void performAction(UUID playerId, String action, int amount) {
        if (gameOver) {
            throw new IllegalStateException("Game is already over");
        }

        ColorClashPlayer player = getPlayer(playerId);

        if (action.equals("CALL_LAST_CARD")) {
            if (player.getHandSize() != 1) {
                throw new IllegalArgumentException("You can only call Last Card with exactly one card in hand");
            }
            player.callLastCard();
            return;
        }

        if (action.startsWith("CATCH:")) {
            UUID targetId = UUID.fromString(action.substring("CATCH:".length()));
            if (targetId.equals(playerId)) {
                throw new IllegalArgumentException("You cannot catch yourself");
            }
            ColorClashPlayer target = getPlayer(targetId);
            if (target.getHandSize() != 1 || target.isCalledLastCard()) {
                throw new IllegalArgumentException(target.getPlayerName() + " has nothing to catch right now");
            }
            dealCardsToPlayer(target, 2);
            return;
        }

        if (!getCurrentPlayer().getPlayerId().equals(playerId)) {
            throw new IllegalStateException("It is not this player's turn");
        }

        if (player.isEliminated()) {
            throw new IllegalStateException("Player is eliminated");
        }

        if (action.startsWith("PLAY:")) {
            String[] parts = action.split(":");
            int cardIndex = Integer.parseInt(parts[1]);
            CardColor chosenColor = parts.length > 2 ? CardColor.valueOf(parts[2]) : null;

            if (cardIndex < 0 || cardIndex >= player.getHand().size()) {
                throw new IllegalArgumentException("Invalid card index");
            }

            ColorClashCard card = player.getHand().get(cardIndex);

            boolean canPlay = false;

            if (drawStack > 0) {
                if (card.type() == CardType.DRAW_TWO || card.type() == CardType.WILD_DRAW_FOUR) {
                    throw new IllegalArgumentException("You must draw " + drawStack + " cards first!");
                }
                canPlay = true;
            } else {
                canPlay = card.isWild() ||
                        card.color() == currentColor ||
                        (card.type() == CardType.NUMBER && card.value() == topCard.value()) ||
                        (card.type() == CardType.SKIP && topCard.type() == CardType.SKIP) ||
                        (card.type() == CardType.REVERSE && topCard.type() == CardType.REVERSE) ||
                        (card.type() == CardType.DRAW_TWO && topCard.type() == CardType.DRAW_TWO);
            }

            if (!canPlay) {
                throw new IllegalArgumentException("Card cannot be played");
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

            checkWinner(player);
            if (gameOver) return;

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
                    int drawn = card.type() == CardType.WILD_DRAW_FOUR ? 4 : 2;
                    drawStack -= drawn;
                    if (drawStack == 0) {
                        nextPlayer();
                    } else {
                        nextPlayer();
                    }
                } else {
                    nextPlayer();
                }
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
            throw new IllegalArgumentException("Unknown action: " + action);
        }
    }

    @Override
    public Object getState(UUID viewerId) {
        return ColorClashState.fromGame(this, viewerId);
    }

    @Override
    public boolean isGameOver() {
        return gameOver;
    }

    public void resetForNewRound() {
        gameOver = false;
        winnerId = null;
        drawStack = 0;
        currentPlayerIndex = 0;
        clockwise = true;

        List<ColorClashCard> allCards = new ArrayList<>();
        for (ColorClashPlayer p : players) {
            allCards.addAll(p.getHand());
            p.clearHand();
        }
        allCards.addAll(discardPile);
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

    public List<ColorClashPlayer> getPlayers() {
        return new ArrayList<>(players);
    }

    public ColorClashPlayer getPlayer(UUID playerId) {
        for (ColorClashPlayer p : players) {
            if (p.getPlayerId().equals(playerId)) return p;
        }
        throw new IllegalArgumentException("Player not found: " + playerId);
    }

    public ColorClashPlayer getCurrentPlayer() {
        if (players.isEmpty()) return null;
        return players.get(currentPlayerIndex);
    }

    public ColorClashCard getTopCard() {
        return topCard;
    }

    public CardColor getCurrentColor() {
        return currentColor;
    }

    public int getDrawStack() {
        return drawStack;
    }

    public boolean isClockwise() {
        return clockwise;
    }

    public ColorClashDeck getDeck() {
        return deck;
    }

    public UUID getWinnerId() {
        return winnerId;
    }

    public void addPlayer(ColorClashPlayer player) {
        players.add(player);
    }

    public boolean hasCalledLastCard(UUID playerId) {
        return getPlayer(playerId).isCalledLastCard();
    }
}