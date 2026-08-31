package com.fizzycoyote.pockettable.network.colorclash;

import com.fizzycoyote.pockettable.engine.colorclash.ColorClashGame;
import com.fizzycoyote.pockettable.engine.colorclash.ColorClashPlayer;
import com.fizzycoyote.pockettable.models.colorclash.ColorClashState;
import com.fizzycoyote.pockettable.network.common.GameMessage;
import com.fizzycoyote.pockettable.network.common.GenericHostServer;
import com.fizzycoyote.pockettable.network.common.MessageType;
import com.google.gson.Gson;

import java.util.UUID;

public class ColorClashHostServer extends GenericHostServer {

    private final Gson gson = new Gson();
    private ColorClashGame game;
    private final UUID hostPlayerId;

    public interface StateChangeListener {
        void onStateChanged(ColorClashState state);
    }
    private StateChangeListener stateListener;

    public ColorClashHostServer(int port, ColorClashGame game, UUID hostPlayerId) {
        super(port);
        this.game = game;
        this.hostPlayerId = hostPlayerId;
    }

    public void setStateListener(StateChangeListener listener) {
        this.stateListener = listener;
    }

    private void notifyStateChanged(ColorClashState state) {
        if (stateListener != null) {
            stateListener.onStateChanged(state);
        }
    }

    public void setGame(ColorClashGame game) {
        this.game = game;
    }

    @Override
    protected void onPlayerJoined(UUID playerId, String playerName, boolean isReconnect) {
        try {
            ColorClashPlayer player = game.getPlayer(playerId);
            player.setPlayerName(playerName);
            if (player.getHandSize() == 0 && !game.isGameOver()) {
                game.dealCardsToPlayer(player, 7);
            }
        } catch (IllegalArgumentException e) {
            ColorClashPlayer newPlayer = new ColorClashPlayer(game.getContext(), playerId);
            newPlayer.setPlayerName(playerName);
            game.addPlayer(newPlayer);
            game.dealCardsToPlayer(newPlayer, 7);
        }
    }

    @Override
    protected void onPlayerDisconnectedPermanently(UUID playerId) {
        try {
            ColorClashPlayer player = game.getPlayer(playerId);
            player.eliminate();
            broadcastState();
        } catch (Exception ignored) {}
    }

    @Override
    protected void onClientMessage(UUID playerId, String message) {
        try {
            game.performAction(playerId, message, 0);
            broadcastState();
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
        ColorClashState state = ColorClashState.fromGame(game, viewerId);
        GameMessage msg = new GameMessage(MessageType.STATE_UPDATE, state);
        return gson.toJson(msg);
    }

    @Override
    public void broadcastState() {
        super.broadcastState();
        ColorClashState state = ColorClashState.fromGame(game, hostPlayerId);
        notifyStateChanged(state);
    }

    public void broadcastGameStart() {
        for (var entry : getClientPlayerMap().entrySet()) {
            ColorClashState state = ColorClashState.fromGame(game, entry.getValue());
            GameMessage msg = new GameMessage(MessageType.GAME_STARTED, state);
            entry.getKey().send(gson.toJson(msg));
        }
        ColorClashState state = ColorClashState.fromGame(game, hostPlayerId);
        notifyStateChanged(state);
    }

    public void stopServer() {
        super.stopServer();
    }
}