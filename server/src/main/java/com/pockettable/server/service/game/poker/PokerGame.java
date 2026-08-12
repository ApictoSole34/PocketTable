package com.pockettable.server.service.game.poker;

import com.pockettable.server.model.game.Card;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PokerGame {

    private final List<PokerPlayer> players;
    private final Deck deck;
    private final List<Card> communityCards = new ArrayList<>();
    private int totalPot = 0;

    private static final int SMALL_BLIND = 50;
    private static final int BIG_BLIND = 100;

    private int dealerIndex = 0;

    private int currentBet = 0;

    private int currentPlayerIndex = 0;

    public PokerGame(List<UUID> playersIds) {

        this.players = playersIds.stream()
                .map(PokerPlayer::new)
                .collect(Collectors.toCollection(ArrayList::new));

        this.deck = new Deck();
        this.deck.shuffle();
    }

    private PokerRound round = PokerRound.PRE_FLOP;

    public List<Card> getCommunityCards() {
        return List.copyOf(communityCards);
    }

    public PokerPlayer getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }

    public void nextPlayer() {

        int size = players.size();

        for (int step = 1; step <= size; step++) {

            int idx = (currentPlayerIndex + step) % size;
            PokerPlayer candidate = players.get(idx);

            if (!candidate.isFolded() && !candidate.isAllIn()) {
                currentPlayerIndex = idx;
                return;
            }
        }
    }

    public int getCurrentBet() {
        return currentBet;
    }

    public PokerRound getRound() {
        return round;
    }

    public PokerPlayer getDealer() {
        return players.get(dealerIndex);
    }

    public void addBet(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException(
                    "Bet amount must be positive"
            );
        }

        this.currentBet += amount;
    }


    public void dealFlop() {

        deck.draw();

        for (int i = 0; i < 3; i++) {
            communityCards.add(deck.draw());
        }

        round = PokerRound.FLOP;
    }

    public void dealRiver() {

        if (round != PokerRound.TURN) {
            throw new IllegalStateException(
                    "Cannot deal river during" + round
            );
        }

        deck.draw();

        communityCards.add(deck.draw());

        round = PokerRound.RIVER;
    }

    public void dealTurn() {

        if (round != PokerRound.FLOP) {
            throw new IllegalStateException(
                    "Cannot deal turn during " + round
            );
        }

        deck.draw();

        communityCards.add(deck.draw());

        round = PokerRound.TURN;
    }

    public List<PokerPlayer> getPlayers() {
        return players;
    }

    public PokerPlayer getPlayer(UUID playerId) {

        return players.stream()
                .filter(player -> player.getPlayerId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Player not found"
                ));
    }

    public void setCurrentBet(int currentBet) {
        this.currentBet = currentBet;
    }

    public Deck getDeck() {
        return deck;
    }

    public void dealInitialCards() {

        for (int i = 0; i < 2; i++) {

            for (PokerPlayer player : players) {

                player.addCard(deck.draw());
            }
        }
    }

    public boolean allPlayersActed() {

        return players.stream()
                .filter(player -> !player.isFolded())
                .allMatch(player -> player.hasActed() || player.isAllIn());
    }

    private void resetBets() {

        players.forEach(PokerPlayer::resetBet);
        currentBet = 0;
    }

    public void resetActedExcept(PokerPlayer player) {

        players.stream()
                .filter(p -> p != player)
                .forEach(PokerPlayer::resetActed);
    }

    public void advanceRound() {

        switch (round) {

            case PRE_FLOP -> {
                dealFlop();
                resetPlayersActed();
                resetBets();
                resetCurrentPlayerAfterDealer();
            }

            case FLOP -> {
                dealTurn();
                resetPlayersActed();
                resetBets();
                resetCurrentPlayerAfterDealer();
            }

            case TURN -> {
                dealRiver();
                resetPlayersActed();
                resetBets();
                resetCurrentPlayerAfterDealer();
            }

            case RIVER -> {
                round = PokerRound.SHOWDOWN;
            }

            case SHOWDOWN -> {
                throw new IllegalStateException(
                        "Game is already in showdown"
                );
            }
        }
    }

    private void resetPlayersActed() {

        players.forEach(PokerPlayer::resetActed);
    }

    private void resetCurrentPlayerAfterDealer() {

        int size = players.size();

        for (int step = 1; step <= size; step++) {

            int idx = (dealerIndex + step) % size;
            PokerPlayer candidate = players.get(idx);

            if (!candidate.isFolded() && !candidate.isAllIn()) {
                currentPlayerIndex = idx;
                return;
            }
        }
    }

    public void postBlinds() {

        int smallBlindIndex;
        int bigBlindIndex;

        if (players.size() == 2) {
            smallBlindIndex = dealerIndex;
            bigBlindIndex = (dealerIndex + 1) % players.size();
        } else {
            smallBlindIndex = (dealerIndex + 1) % players.size();
            bigBlindIndex = (dealerIndex + 2) % players.size();
        }

        PokerPlayer smallBlind = players.get(smallBlindIndex);
        PokerPlayer bigBlind = players.get(bigBlindIndex);

        int smallBlindPosted = smallBlind.removeChipsUpTo(SMALL_BLIND);
        smallBlind.addBet(smallBlindPosted);
        addToPot(smallBlindPosted);

        int bigBlindPosted = bigBlind.removeChipsUpTo(BIG_BLIND);
        bigBlind.addBet(bigBlindPosted);
        addToPot(bigBlindPosted);

        currentBet = BIG_BLIND;

        currentPlayerIndex = (bigBlindIndex + 1) % players.size();
    }

    public void moveDealer() {

        dealerIndex++;

        if (dealerIndex >= players.size()) {
            dealerIndex = 0;
        }
    }

    public void resetForNewHand() {

        communityCards.clear();
        totalPot = 0;

        players.forEach(player -> {
            player.clearHand();
            player.resetBet();
            player.resetActed();
            player.resetFolded();
            player.resetAllIn();
            player.resetTotalContribution();

        });

        currentBet = 0;
        currentPlayerIndex = 0;
        round = PokerRound.PRE_FLOP;

        moveDealer();
        deck.reset();
        deck.shuffle();
        startNewHand();
    }

    public void startNewHand() {

        postBlinds();

        dealInitialCards();
    }

    public void addToPot(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        this.totalPot += amount;
    }

    public int getTotalPot() {
        return totalPot;
    }

    public List<SidePot> buildSidePots() {
        List<SidePot> sidePots = new ArrayList<>();

        List<PokerPlayer> activePlayers = players.stream()
                .filter(p -> !p.isFolded())
                .toList();

        if (activePlayers.isEmpty()) {
            return sidePots;
        }

        int deadMoney = players.stream()
                .filter(PokerPlayer::isFolded)
                .mapToInt(PokerPlayer::getTotalContribution)
                .sum();

        List<PokerPlayer> sorted = activePlayers.stream()
                .sorted(Comparator.comparingInt(PokerPlayer::getTotalContribution))
                .toList();

        int prev = 0;
        boolean first = true;

        for (int i = 0; i < sorted.size(); i++) {
            int current = sorted.get(i).getTotalContribution();

            if (current == prev) {
                continue;
            }

            List<UUID> eligible = sorted.stream()
                    .filter(p -> p.getTotalContribution() >= current)
                    .map(PokerPlayer::getPlayerId)
                    .toList();

            int count = eligible.size();
            int amount = (current - prev) * count;

            if (first) {
                amount += deadMoney;
                first = false;
            }

            if (amount > 0) {
                sidePots.add(new SidePot(amount, eligible));
            }

            prev = current;
        }

        return sidePots;
    }

}
