package com.fizzycoyote.pockettable.network.mafia;

import com.fizzycoyote.pockettable.models.mafia.MafiaState;
import com.fizzycoyote.pockettable.network.common.GameMessage;
import com.fizzycoyote.pockettable.network.common.GenericGameClient;
import com.fizzycoyote.pockettable.network.common.MessageType;
import com.google.gson.Gson;

import java.net.URI;
import java.util.UUID;

public class MafiaClient extends GenericGameClient {

    private final Gson gson = new Gson();
    private final UUID playerId;
    private MessageListener listener;

    public interface MessageListener {
        void onState(MafiaState state);
        void onGameStarted();
        void onGameOver();
        void onReconnecting(int attempt);
        void onReconnected();
        void onReconnectFailed();
        void onActionError(String message);
    }

    public MafiaClient(URI uri, UUID playerId, String playerName, String roomCode) {
        super(uri, playerId, playerName, roomCode);
        this.playerId = playerId;
    }

    public void setListener(MessageListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onRawMessage(String message) {
        if (listener == null) return;

        if (message.startsWith("ERROR:")) {
            listener.onActionError(message.substring(6).trim());
            return;
        }

        try {
            GameMessage gameMessage = gson.fromJson(message, GameMessage.class);
            MessageType type = gameMessage.getType();

            if (type == MessageType.STATE_UPDATE || type == MessageType.GAME_STARTED) {
                MafiaState state = gson.fromJson(
                        gson.toJson(gameMessage.getPayload()),
                        MafiaState.class
                );
                listener.onState(state);

                if (type == MessageType.GAME_STARTED) {
                    listener.onGameStarted();
                }
            } else if (type == MessageType.GAME_OVER) {
                listener.onGameOver();
            }
        } catch (Exception e) {
            try {
                MafiaState state = gson.fromJson(message, MafiaState.class);
                listener.onState(state);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    protected void onReconnecting(int attempt) {
        if (listener != null) listener.onReconnecting(attempt);
    }

    @Override
    protected void onReconnected() {
        if (listener != null) listener.onReconnected();
        send("GET_STATE");
    }

    @Override
    protected void onReconnectFailed() {
        if (listener != null) listener.onReconnectFailed();
    }

    public void sendAction(String action, int amount) {
        send(action);
    }
}