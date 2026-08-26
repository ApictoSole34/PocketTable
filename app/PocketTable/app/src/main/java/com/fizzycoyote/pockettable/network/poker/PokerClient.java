package com.fizzycoyote.pockettable.network.poker;

import com.fizzycoyote.pockettable.models.poker.PokerActionRequest;
import com.fizzycoyote.pockettable.models.poker.PokerGameState;
import com.fizzycoyote.pockettable.network.common.GameMessage;
import com.fizzycoyote.pockettable.network.common.GenericGameClient;
import com.fizzycoyote.pockettable.network.common.MessageType;
import com.google.gson.Gson;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * WebSocket client for a poker player.
 * Connects to the host server and exchanges JSON messages.
 *
 * <p>Receives {@link PokerGameState} updates and sends {@link PokerActionRequest}.</p>
 *
 * <p>Uses the underlying WebSocket ping/pong mechanism to detect dead connections
 * quickly, and automatically attempts to reconnect with exponential backoff if
 * the connection is lost unexpectedly (as opposed to being closed intentionally
 * via {@link #requestClose()}).</p>
 *
 * @see PokerHostServer
 * @see PokerGameState
 * @see PokerActionRequest
 */
public class PokerClient extends GenericGameClient {

    private final Gson gson = new Gson();
    private final UUID playerId;
    private MessageListener listener;

    public interface MessageListener {
        void onState(PokerGameState state);
    }

    public interface GameMessageListener extends MessageListener {
        void onGameStarted();
        void onGameOver();
        void onReconnecting(int attempt);
        void onReconnected();
        void onReconnectFailed();
        void onActionError(String message);
    }

    public PokerClient(URI uri, UUID playerId, String playerName) {
        super(uri, playerId, playerName);
        this.playerId = playerId;
    }

    public void setListener(MessageListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onRawMessage(String message) {
        System.out.println("CLIENT RECEIVED: " + message);
        if (listener == null) return;

        if (message.startsWith(PokerHostServer.ERROR_PREFIX)) {
            String errorMsg = message.substring(PokerHostServer.ERROR_PREFIX.length()).trim();
            if (listener instanceof GameMessageListener) {
                ((GameMessageListener) listener).onActionError(errorMsg);
            }
            return;
        }

        try {
            GameMessage gameMessage = gson.fromJson(message, GameMessage.class);
            MessageType type = gameMessage.getType();

            if (type == MessageType.STATE_UPDATE || type == MessageType.GAME_STARTED) {
                PokerGameState state = gson.fromJson(
                        gson.toJson(gameMessage.getPayload()),
                        PokerGameState.class
                );
                listener.onState(state);

                if (type == MessageType.GAME_STARTED && listener instanceof GameMessageListener) {
                    ((GameMessageListener) listener).onGameStarted();
                }
            } else if (type == MessageType.GAME_OVER) {
                if (listener instanceof GameMessageListener) {
                    ((GameMessageListener) listener).onGameOver();
                }
            }
        } catch (Exception e) {
            try {
                PokerGameState state = gson.fromJson(message, PokerGameState.class);
                listener.onState(state);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    protected void onReconnecting(int attempt) {
        if (listener instanceof GameMessageListener) {
            ((GameMessageListener) listener).onReconnecting(attempt);
        }
    }

    @Override
    protected void onReconnected() {
        if (listener instanceof GameMessageListener) {
            ((GameMessageListener) listener).onReconnected();
        }
        send("GET_STATE");
    }

    @Override
    protected void onReconnectFailed() {
        if (listener instanceof GameMessageListener) {
            ((GameMessageListener) listener).onReconnectFailed();
        }
    }

    public void sendAction(PokerActionRequest request) {
        System.out.println("CLIENT SENDING: " + gson.toJson(request));
        send(gson.toJson(request));
    }
}