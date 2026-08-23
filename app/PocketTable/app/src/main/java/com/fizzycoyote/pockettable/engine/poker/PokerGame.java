package com.fizzycoyote.pockettable.engine.poker;

import com.fizzycoyote.pockettable.engine.common.Card;
import com.fizzycoyote.pockettable.engine.common.GameEngine;
import com.fizzycoyote.pockettable.models.poker.PokerGameState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Core poker game engine for Texas Hold'em.
 * Manages players, bets, blinds, community cards, and round progression.
 *
 * <p>Key responsibilities:
 * <ul>
 *   <li>Track player chips, hands, and status</li>
 *   <li>Process actions: FOLD, CHECK, BET, CALL, RAISE</li>
 *   <li>Advance rounds: PRE_FLOP → FLOP → TURN → RIVER → SHOWDOWN</li>
 *   <li>Determine winner using {@link PokerHandEvaluator}</li>
 * </ul>
 *
 * @author Your Name
 * @version 1.0
 * @see PokerPlayer
 * @see PokerHandEvaluator
 * @see PokerGameState
 */
public class PokerGame implements GameEngine {

    private final List<PokerPlayer> players;
    private final Deck deck;
    private final List<Card> communityCards = new ArrayList<>();
    private int totalPot = 0;

    private int smallBlind = 50;
    private int bigBlind = 100;
    private int startingChips = 1000;

    private int dealerIndex = 0;
    private int currentBet = 0;
    private int currentPlayerIndex = 0;
    private PokerRound round = PokerRound.PRE_FLOP;
    private boolean gameOver = false;
    private boolean tournamentOver = false;

    private UUID winnerId = null;
    private String winnerHandDesc = null;

    private final PokerHandEvaluator handEvaluator = new PokerHandEvaluator();

    public PokerGame(String roomCode, List<UUID> playersIds) {
        this.players = new ArrayList<>();
        for (UUID id : playersIds) {
            this.players.add(new PokerPlayer(id, startingChips));
        }
        this.deck = new Deck();
        this.deck.shuffle();
    }

    public int getSmallBlind() { return smallBlind; }
    public int getBigBlind() { return bigBlind; }
    public int getStartingChips() { return startingChips; }
    public boolean isTournamentOver() { return tournamentOver; }

    public UUID getChampionId() {
        if (!tournamentOver || players.isEmpty()) return null;
        return players.get(0).getPlayerId();
    }

    public void applySettings(int smallBlind, int bigBlind, int startingChips) {
        this.smallBlind = smallBlind;
        this.bigBlind = bigBlind;
        this.startingChips = startingChips;
        for (PokerPlayer player : players) {
            player.resetChips(startingChips);
        }
    }

    @Override
    public void startGame() {
        removeEliminatedPlayers();
        startNewHand();
    }

    /**
     * Processes a player's action during their turn.
     *
     * @param playerId ID of the acting player
     * @param action   Action type (FOLD, CHECK, BET, CALL, RAISE)
     * @param amount   Bet amount (required for BET and RAISE)
     * @throws IllegalStateException if game is in SHOWDOWN, wrong turn, or player already folded/all-in
     * @throws IllegalArgumentException if amount is invalid
     */
    @Override
    public void performAction(UUID playerId, String action, int amount) {
        PokerPlayer player = getPlayer(playerId);

        if (round == PokerRound.SHOWDOWN) {
            throw new IllegalStateException("Game is already in showdown");
        }

        if (!getCurrentPlayer().getPlayerId().equals(playerId)) {
            throw new IllegalStateException("It is not this player's turn");
        }

        if (player.isFolded()) {
            throw new IllegalStateException("Player has already folded");
        }

        if (player.isAllIn()) {
            throw new IllegalStateException("Player is already all-in and cannot act");
        }

        PokerAction pokerAction = PokerAction.valueOf(action);

        switch (pokerAction) {

            case FOLD:
                player.fold();
                break;

            case CHECK:
                if (player.getCurrentBet() != currentBet) {
                    throw new IllegalStateException("Cannot check while current bet is higher");
                }
                break;

            case BET:
                if (currentBet != 0) {
                    throw new IllegalStateException("Cannot bet when there is already a bet");
                }
                if (amount <= 0) {
                    throw new IllegalArgumentException("Bet amount must be positive");
                }
                int actualBet = player.removeChipsUpTo(amount);
                player.addBet(actualBet);
                currentBet = player.getCurrentBet();
                resetActedExcept(player);
                addToPot(actualBet);
                break;

            case CALL:
                int amountToCall = currentBet - player.getCurrentBet();
                if (amountToCall <= 0) {
                    throw new IllegalStateException("Nothing to call");
                }
                int actualCall = player.removeChipsUpTo(amountToCall);
                player.addBet(actualCall);
                addToPot(actualCall);
                break;

            case RAISE:
                if (amount <= currentBet) {
                    throw new IllegalArgumentException("Raise must be higher than current bet");
                }
                int amountToAdd = amount - player.getCurrentBet();
                if (amountToAdd <= 0) {
                    throw new IllegalStateException("Player has already bet enough");
                }
                int actualRaise = player.removeChipsUpTo(amountToAdd);
                player.addBet(actualRaise);
                addToPot(actualRaise);
                boolean isFullRaise = player.getCurrentBet() > currentBet;
                if (isFullRaise) {
                    currentBet = player.getCurrentBet();
                    resetActedExcept(player);
                }
                break;
        }

        player.markActed();

        int activePlayers = 0;
        for (PokerPlayer p : players) {
            if (!p.isFolded()) activePlayers++;
        }

        if (activePlayers == 1) {
            PokerPlayer winner = null;
            for (PokerPlayer p : players) {
                if (!p.isFolded()) {
                    winner = p;
                    break;
                }
            }
            if (winner != null) {
                winnerId = winner.getPlayerId();
                winnerHandDesc = "Winner (others folded)";
                awardPotToWinner(winner);
                gameOver = true;
            }
            return;
        }

        if (allPlayersActed()) {
            advanceUntilActionOrShowdown();
        } else {
            nextPlayer();
        }
    }

    @Override
    public Object getState(UUID viewerId) {
        return PokerGameState.fromGame(this, viewerId);
    }

    @Override
    public boolean isGameOver() {
        return gameOver;
    }

    private void advanceUntilActionOrShowdown() {
        while (true) {
            advanceRound();

            if (round == PokerRound.SHOWDOWN) {
                finishGame();
                return;
            }

            int playersWhoCanAct = 0;
            for (PokerPlayer p : players) {
                if (!p.isFolded() && !p.isAllIn()) {
                    playersWhoCanAct++;
                }
            }

            if (playersWhoCanAct >= 2) {
                return;
            }
        }
    }

    private void finishGame() {
        List<PokerPlayer> activePlayers = new ArrayList<>();
        for (PokerPlayer p : players) {
            if (!p.isFolded()) {
                activePlayers.add(p);
            }
        }

        if (activePlayers.isEmpty()) {
            return;
        }

        if (activePlayers.size() == 1) {
            PokerPlayer winner = activePlayers.get(0);
            winnerId = winner.getPlayerId();
            winnerHandDesc = "Winner (others folded)";
            winner.addChips(totalPot);
            totalPot = 0;
            gameOver = true;
            return;
        }

        if (round != PokerRound.SHOWDOWN) {
            return;
        }

        List<SidePot> sidePots = buildSidePots();
        if (sidePots.isEmpty()) {
            SidePot mainPot = new SidePot(totalPot, activePlayers.stream()
                    .map(PokerPlayer::getPlayerId)
                    .collect(Collectors.toList()));
            sidePots.add(mainPot);
        }

        for (SidePot pot : sidePots) {
            List<PokerPlayer> eligible = new ArrayList<>();
            for (UUID id : pot.getEligiblePlayerIds()) {
                eligible.add(getPlayer(id));
            }

            WinnerResult result = determineWinnerWithHand(eligible);
            PokerPlayer winner = result.player();
            PokerHand hand = result.hand();

            winner.addChips(pot.getAmount());

            if (winnerId == null) {
                winnerId = winner.getPlayerId();
                winnerHandDesc = hand.rank().name();
            }
        }

        totalPot = 0;
        gameOver = true;
    }

    public record WinnerResult(PokerPlayer player, PokerHand hand) {}

    public WinnerResult determineWinnerWithHand(List<PokerPlayer> eligiblePlayers) {
        PokerPlayer winner = null;
        PokerHand bestHand = null;

        for (PokerPlayer player : eligiblePlayers) {
            if (player.isFolded()) continue;

            List<Card> cards = new ArrayList<>(player.getHand());
            cards.addAll(communityCards);

            PokerHand hand = handEvaluator.evaluateBest(cards);

            if (bestHand == null || hand.compareTo(bestHand) > 0) {
                bestHand = hand;
                winner = player;
            }
        }

        if (winner == null || bestHand == null) {
            throw new IllegalStateException("No eligible active players");
        }

        return new WinnerResult(winner, bestHand);
    }

    private void awardPotToWinner(PokerPlayer winner) {
        winner.addChips(totalPot);
        totalPot = 0;
        gameOver = true;
    }

    public UUID getWinnerId() {return winnerId;}

    public String getWinnerHandDescription() {return winnerHandDesc;}

    public List<Card> getCommunityCards() {
        return new ArrayList<>(communityCards);
    }

    /*  Only For testing **/
    void setCommunityCardsForTest(List<Card> cards) {
        this.communityCards.clear();
        this.communityCards.addAll(cards);
    }

    public PokerPlayer getCurrentPlayer() {
        if (players.isEmpty()) return null;
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
        if (players.isEmpty()) return null;
        return players.get(dealerIndex);
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
            throw new IllegalStateException("Cannot deal river during " + round);
        }
        deck.draw();
        communityCards.add(deck.draw());
        round = PokerRound.RIVER;
    }

    public void dealTurn() {
        if (round != PokerRound.FLOP) {
            throw new IllegalStateException("Cannot deal turn during " + round);
        }
        deck.draw();
        communityCards.add(deck.draw());
        round = PokerRound.TURN;
    }

    public List<PokerPlayer> getPlayers() {
        return new ArrayList<>(players);
    }

    public void addPlayer(PokerPlayer player) {
        players.add(player);
    }

    public PokerPlayer getPlayer(UUID playerId) {
        for (PokerPlayer p : players) {
            if (p.getPlayerId().equals(playerId)) {
                return p;
            }
        }
        throw new IllegalArgumentException("Player not found");
    }

    public void setCurrentBet(int currentBet) {
        this.currentBet = currentBet;
    }

    public void dealInitialCards() {
        for (int i = 0; i < 2; i++) {
            for (PokerPlayer player : players) {
                player.addCard(deck.draw());
            }
        }
    }

    public boolean allPlayersActed() {
        for (PokerPlayer player : players) {
            if (!player.isFolded()) {
                if (!player.hasActed() && !player.isAllIn()) {
                    return false;
                }
            }
        }
        return true;
    }

    private void resetBets() {
        for (PokerPlayer player : players) {
            player.resetBet();
        }
        currentBet = 0;
    }

    public void resetActedExcept(PokerPlayer player) {
        for (PokerPlayer p : players) {
            if (p != player) {
                p.resetActed();
            }
        }
    }

    public void advanceRound() {
        System.out.println("ADVANCE ROUND: from " + round + " to next");
        switch (round) {
            case PRE_FLOP:
                dealFlop();
                resetPlayersActed();
                resetBets();
                resetCurrentPlayerAfterDealer();
                break;
            case FLOP:
                dealTurn();
                resetPlayersActed();
                resetBets();
                resetCurrentPlayerAfterDealer();
                break;
            case TURN:
                dealRiver();
                resetPlayersActed();
                resetBets();
                resetCurrentPlayerAfterDealer();
                break;
            case RIVER:
                round = PokerRound.SHOWDOWN;
                break;
            case SHOWDOWN:
                throw new IllegalStateException("Game is already in showdown");
        }
    }

    private void resetPlayersActed() {
        for (PokerPlayer player : players) {
            player.resetActed();
        }
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

        PokerPlayer smallBlindPlayer = players.get(smallBlindIndex);
        PokerPlayer bigBlindPlayer = players.get(bigBlindIndex);

        int smallBlindPosted = smallBlindPlayer.removeChipsUpTo(smallBlind);
        smallBlindPlayer.addBet(smallBlindPosted);
        addToPot(smallBlindPosted);

        int bigBlindPosted = bigBlindPlayer.removeChipsUpTo(bigBlind);
        bigBlindPlayer.addBet(bigBlindPosted);
        addToPot(bigBlindPosted);

        currentBet = bigBlind;
        currentPlayerIndex = (bigBlindIndex + 1) % players.size();
    }

    public void moveDealer() {
        dealerIndex++;
        if (dealerIndex >= players.size()) {
            dealerIndex = 0;
        }
    }

    private void removeEliminatedPlayers() {
        players.removeIf(p -> p.getChips() < smallBlind);

        if (dealerIndex >= players.size() && !players.isEmpty()) {
            dealerIndex = 0;
        }

        if (players.size() <= 1) {
            tournamentOver = true;
        }
    }

    public void resetForNewHand() {
        removeEliminatedPlayers();
        if (tournamentOver) return;

        communityCards.clear();
        totalPot = 0;
        gameOver = false;
        winnerId = null;
        winnerHandDesc = null;

        for (PokerPlayer player : players) {
            player.clearHand();
            player.resetBet();
            player.resetActed();
            player.resetFolded();
            player.resetAllIn();
            player.resetTotalContribution();
        }

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

        List<PokerPlayer> activePlayers = new ArrayList<>();
        for (PokerPlayer p : players) {
            if (!p.isFolded()) {
                activePlayers.add(p);
            }
        }

        if (activePlayers.isEmpty()) {
            return sidePots;
        }

        int deadMoney = 0;
        for (PokerPlayer p : players) {
            if (p.isFolded()) {
                deadMoney += p.getTotalContribution();
            }
        }

        Collections.sort(activePlayers, new Comparator<PokerPlayer>() {
            @Override
            public int compare(PokerPlayer p1, PokerPlayer p2) {
                return Integer.compare(p1.getTotalContribution(), p2.getTotalContribution());
            }
        });

        int prev = 0;
        boolean first = true;

        for (int i = 0; i < activePlayers.size(); i++) {
            int current = activePlayers.get(i).getTotalContribution();

            if (current == prev) {
                continue;
            }

            List<UUID> eligible = new ArrayList<>();
            for (PokerPlayer p : activePlayers) {
                if (p.getTotalContribution() >= current) {
                    eligible.add(p.getPlayerId());
                }
            }

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
