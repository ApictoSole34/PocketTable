package com.fizzycoyote.pockettable.network.poker;

import com.fizzycoyote.pockettable.engine.poker.PokerGame;
import com.fizzycoyote.pockettable.engine.poker.PokerPlayer;
import com.fizzycoyote.pockettable.models.poker.PokerActionRequest;
import com.fizzycoyote.pockettable.models.poker.PokerGameState;
import com.fizzycoyote.pockettable.network.common.GameMessage;
import com.fizzycoyote.pockettable.network.common.GenericHostServer;
import com.fizzycoyote.pockettable.network.common.MessageType;
import com.google.gson.Gson;

import java.util.UUID;


/**
 * WebSocket server for the poker game host.
 * Manages all connected clients, broadcasts game state, and processes player actions.
 *
 * <p>Each client connection is mapped to a {@link PokerPlayer}.</p>
 *
 * @see PokerClient
 * @see PokerGame
 * @see PokerGameState
 */
public class PokerHostServer extends GenericHostServer {

    private final Gson gson = new Gson();
    private PokerGame game;
    private final UUID hostPlayerId;

    public interface StateChangeListener {
        void onStateChanged(PokerGameState state);
    }
    private StateChangeListener stateListener;

    public PokerHostServer(int port, PokerGame game, UUID hostPlayerId, String roomCode) {
        super(port, roomCode);
        this.game = game;
        this.hostPlayerId = hostPlayerId;
    }

    public void setStateListener(StateChangeListener listener) {
        this.stateListener = listener;
    }

    private void notifyStateChanged(PokerGameState state) {
        if (stateListener != null) {
            stateListener.onStateChanged(state);
        }
    }

    public void setGame(PokerGame game) {
        this.game = game;
    }

    @Override
    protected void onPlayerJoined(UUID playerId, String playerName, boolean isReconnect) {
        try {
            PokerPlayer player = game.getPlayer(playerId);
            player.setPlayerName(playerName);
        } catch (IllegalArgumentException e) {
            PokerPlayer newPlayer = new PokerPlayer(game.getContext(), playerId, game.getStartingChips());
            newPlayer.setPlayerName(playerName);
            game.addPlayer(newPlayer);
        }
    }

    @Override
    protected void onPlayerDisconnectedPermanently(UUID playerId) {
        try {
            PokerPlayer player = game.getPlayer(playerId);
            if (!player.isFolded() && !game.isGameOver()) {
                player.fold();
                PokerGameState state = PokerGameState.fromGame(game, hostPlayerId);
                notifyStateChanged(state);
            }
        } catch (Exception ignored) {}
    }

    @Override
    protected void onClientMessage(UUID playerId, String message) {
        try {
            PokerActionRequest request = gson.fromJson(message, PokerActionRequest.class);
            game.performAction(request.playerId(), request.action().name(), request.amount());
            broadcastState();
            PokerGameState state = PokerGameState.fromGame(game, hostPlayerId);
            notifyStateChanged(state);
        } catch (Exception e) {
            e.printStackTrace();
            for (var entry : getClientPlayerMap().entrySet()) {
                if (entry.getValue().equals(playerId)) {
                    entry.getKey().send(ERROR_PREFIX + e.getMessage());
                    break;
                }
            }
        }
    }

    @Override
    protected String buildStateMessage(UUID viewerId) {
        PokerGameState state = PokerGameState.fromGame(game, viewerId);
        GameMessage msg = new GameMessage(MessageType.STATE_UPDATE, state);
        return gson.toJson(msg);
    }

    @Override
    public void broadcastState() {
        super.broadcastState();
        PokerGameState state = PokerGameState.fromGame(game, hostPlayerId);
        notifyStateChanged(state);
    }

    public void broadcastGameStart() {
        for (var entry : getClientPlayerMap().entrySet()) {
            PokerGameState state = PokerGameState.fromGame(game, entry.getValue());
            GameMessage msg = new GameMessage(MessageType.GAME_STARTED, state);
            entry.getKey().send(gson.toJson(msg));
        }
        PokerGameState state = PokerGameState.fromGame(game, hostPlayerId);
        notifyStateChanged(state);
    }

    public void startNextHand() {
        game.resetForNewHand();
        broadcastGameStart();
    }
}