package com.fizzycoyote.pockettable.network.mafia;

import com.fizzycoyote.pockettable.engine.mafia.MafiaGame;
import com.fizzycoyote.pockettable.engine.mafia.MafiaPhase;
import com.fizzycoyote.pockettable.engine.mafia.MafiaPlayer;
import com.fizzycoyote.pockettable.engine.mafia.TimedMafiaGame;
import com.fizzycoyote.pockettable.models.mafia.MafiaState;
import com.fizzycoyote.pockettable.network.common.GameMessage;
import com.fizzycoyote.pockettable.network.common.GenericHostServer;
import com.fizzycoyote.pockettable.network.common.MessageType;
import com.google.gson.Gson;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MafiaHostServer extends GenericHostServer {

    private final Gson gson = new Gson();
    private MafiaGame game;
    private final UUID hostPlayerId;

    public interface StateChangeListener {
        void onStateChanged(MafiaState state);
    }

    private StateChangeListener stateListener;

    public MafiaHostServer(int port, MafiaGame game, UUID hostPlayerId) {
        super(port);
        this.game = game;
        this.hostPlayerId = hostPlayerId;
        setupTimerListener();
    }

    public void setStateListener(StateChangeListener listener) {
        this.stateListener = listener;
    }

    public void setGame(MafiaGame game) {
        this.game = game;
        setupTimerListener();
    }

    private void setupTimerListener() {
        if (game instanceof TimedMafiaGame timedGame) {
            timedGame.setTimerListener(new TimedMafiaGame.TimerListener() {
                @Override
                public void onTick(int remainingSeconds) {}

                @Override
                public void onPhaseExpired() {
                    broadcastState();
                    rescheduleTimerIfNeeded();
                }
            });
        }
    }

    private void notifyStateChanged(MafiaState state) {
        if (stateListener != null) {
            stateListener.onStateChanged(state);
        }
    }

    private MafiaState snapshotFor(UUID viewerId) {
        return (MafiaState) game.getState(viewerId);
    }

    @Override
    protected void onPlayerJoined(UUID playerId, String playerName, boolean isReconnect) {
        try {
            MafiaPlayer player = game.getPlayer(playerId);
            player.setPlayerName(playerName);
            player.setConnected(true);
        } catch (IllegalArgumentException e) {
            MafiaPlayer newPlayer = new MafiaPlayer(playerId);
            newPlayer.setPlayerName(playerName);
            game.addPlayer(newPlayer);
        }
    }

    @Override
    protected void onPlayerDisconnectedPermanently(UUID playerId) {
        try {
            game.handlePlayerLeft(playerId);
            broadcastState();
            rescheduleTimerIfNeeded();
        } catch (Exception ignored) {}
    }

    @Override
    protected void onClientMessage(UUID playerId, String message) {
        try {
            MafiaPhase phaseBefore = game.getPhase();
            game.performAction(playerId, message, 0);

            broadcastState();

            if (phaseBefore != game.getPhase() || game.isGameOver()) {
                rescheduleTimerIfNeeded();
            }
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
        MafiaState state = snapshotFor(viewerId);
        GameMessage msg = new GameMessage(MessageType.STATE_UPDATE, state);
        return gson.toJson(msg);
    }

    @Override
    public void broadcastState() {
        super.broadcastState();
        MafiaState state = snapshotFor(hostPlayerId);
        notifyStateChanged(state);
    }

    public void broadcastGameStart() {
        for (var entry : getClientPlayerMap().entrySet()) {
            MafiaState state = snapshotFor(entry.getValue());
            GameMessage msg = new GameMessage(MessageType.GAME_STARTED, state);
            entry.getKey().send(gson.toJson(msg));
        }
        MafiaState state = snapshotFor(hostPlayerId);
        notifyStateChanged(state);
        rescheduleTimerIfNeeded();
    }

    public void broadcastGameOver() {
        GameMessage msg = new GameMessage(MessageType.GAME_OVER, null);
        String json = gson.toJson(msg);
        for (var entry : getClientPlayerMap().entrySet()) {
            entry.getKey().send(json);
        }
    }

    private void rescheduleTimerIfNeeded() {
        if (game instanceof TimedMafiaGame timedGame) {
            timedGame.onPhaseChangedStartTimer();
        }
    }

    public void stopServer() {
        super.stopServer();
        if (game instanceof TimedMafiaGame timedGame) {
            timedGame.stopPhaseTimer();
        }
    }
}