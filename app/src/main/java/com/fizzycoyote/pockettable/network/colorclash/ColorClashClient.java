package com.fizzycoyote.pockettable.network.colorclash;


import com.fizzycoyote.pockettable.models.colorclash.ColorClashState;
import com.fizzycoyote.pockettable.network.common.GameMessage;
import com.fizzycoyote.pockettable.network.common.GenericGameClient;
import com.fizzycoyote.pockettable.network.common.MessageType;
import com.google.gson.Gson;

import java.net.URI;
import java.util.UUID;

public class ColorClashClient extends GenericGameClient {

    private final Gson gson = new Gson();
    private final UUID playerId;
    private MessageListener listener;

    public interface MessageListener {
        void onState(ColorClashState state);
        void onGameStarted();
        void onGameOver();
        void onReconnecting(int attempt);
        void onReconnected();
        void onReconnectFailed();
        void onActionError(String message);
    }

    public ColorClashClient(URI uri, UUID playerId, String playerName) {
        super(uri, playerId, playerName);
        this.playerId = playerId;
    }

    public void setListener(MessageListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onRawMessage(String message) {
        if (listener == null) return;

        if (message.startsWith("ERROR:")) {
            String errorMsg = message.substring(6).trim();
            listener.onActionError(errorMsg);
            return;
        }

        try {
            GameMessage gameMessage = gson.fromJson(message, GameMessage.class);
            MessageType type = gameMessage.getType();

            if (type == MessageType.STATE_UPDATE || type == MessageType.GAME_STARTED) {
                ColorClashState state = gson.fromJson(
                        gson.toJson(gameMessage.getPayload()),
                        ColorClashState.class
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
                ColorClashState state = gson.fromJson(message, ColorClashState.class);
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