package com.fizzycoyote.pockettable.network;

import com.fizzycoyote.pockettable.models.poker.PokerActionRequest;
import com.fizzycoyote.pockettable.models.poker.PokerGameState;
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
 * @see PokerHostServer
 * @see PokerGameState
 * @see PokerActionRequest
 */
public class PokerClient extends WebSocketClient {
    private final Gson gson = new Gson();
    private final UUID playerId;
    private final String playerName;
    private MessageListener listener;

    public interface MessageListener {
        void onState(PokerGameState state);
    }

    public interface GameMessageListener extends MessageListener {
        void onGameStarted();
        void onGameOver();
    }

    public PokerClient(URI uri, UUID playerId, String playerName) {
        super(uri, getHeaders(playerId, playerName));
        this.playerId = playerId;
        this.playerName = playerName;
    }

    private static Map<String, String> getHeaders(UUID playerId, String playerName) {
        Map<String, String> headers = new HashMap<>();
        headers.put("playerId", playerId.toString());
        headers.put("playerName", playerName != null ? playerName : "Player");
        return headers;
    }

    public void setListener(MessageListener listener) {
        this.listener = listener;
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        send("GET_STATE");
    }

    @Override
    public void onMessage(String message) {
        System.out.println("CLIENT RECEIVED: " + message);
        if (listener == null) return;
        try {
            GameMessage gameMessage = gson.fromJson(message, GameMessage.class);
            MessageType type = gameMessage.getType();
            if (type == MessageType.STATE_UPDATE) {
                PokerGameState state = gson.fromJson(gson.toJson(gameMessage.getPayload()), PokerGameState.class);
                listener.onState(state);
            } else if (type == MessageType.GAME_STARTED) {
                PokerGameState state = gson.fromJson(gson.toJson(gameMessage.getPayload()), PokerGameState.class);
                listener.onState(state);
                if (listener instanceof GameMessageListener) {
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
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("CLIENT: connection closed code=" + code + " reason=" + reason);
    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
    }

    public void sendAction(PokerActionRequest request) {
        System.out.println("CLIENT SENDING: " + gson.toJson(request));
        send(gson.toJson(request));
    }

    public void close() {
        try {
            super.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
