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
 * <p>Uses the underlying WebSocket ping/pong mechanism ({@link #setConnectionLostTimeout})
 * to detect dead connections quickly, and automatically attempts to reconnect with
 * exponential backoff if the connection is lost unexpectedly (as opposed to being
 * closed intentionally via {@link #requestClose()}).</p>
 *
 * @see PokerHostServer
 * @see PokerGameState
 * @see PokerActionRequest
 */
public class PokerClient extends WebSocketClient {
    private static final int CONNECTION_LOST_TIMEOUT_SECONDS = 20;
    private static final int MAX_RECONNECT_ATTEMPTS = 10;
    private static final long BASE_RECONNECT_DELAY_MS = 2000;
    private static final long MAX_RECONNECT_DELAY_MS = 15000;

    private final Gson gson = new Gson();
    private final UUID playerId;
    private final String playerName;
    private MessageListener listener;

    private volatile boolean manualClose = false;
    private volatile boolean reconnecting = false;
    private Thread reconnectThread;

    public interface MessageListener {
        void onState(PokerGameState state);
    }

    public interface GameMessageListener extends MessageListener {
        void onGameStarted();
        void onGameOver();

        default void onReconnecting(int attempt) {}
        default void onReconnected() {}
        default void onReconnectFailed() {}
        default void onActionError(String message) {}
    }

    public PokerClient(URI uri, UUID playerId, String playerName) {
        super(uri, getHeaders(playerId, playerName));
        this.playerId = playerId;
        this.playerName = playerName;
        setConnectionLostTimeout(CONNECTION_LOST_TIMEOUT_SECONDS);
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
        System.out.println("CLIENT: connection closed code=" + code + " reason=" + reason + " manualClose=" + manualClose);
        if (!manualClose) {
            startReconnectLoop();
        }
    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
    }

    public void sendAction(PokerActionRequest request) {
        System.out.println("CLIENT SENDING: " + gson.toJson(request));
        send(gson.toJson(request));
    }

    /**
     * Call this when the player intentionally leaves the game (e.g. leaving the
     * activity). Prevents the automatic reconnect loop from starting.
     */
    public void requestClose() {
        manualClose = true;
        if (reconnectThread != null) {
            reconnectThread.interrupt();
        }
        close();
    }

    public void close() {
        try {
            super.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startReconnectLoop() {
        if (reconnecting) return;
        reconnecting = true;

        reconnectThread = new Thread(() -> {
            int attempt = 0;
            while (attempt < MAX_RECONNECT_ATTEMPTS && !manualClose) {
                attempt++;
                long delay = Math.min(
                        BASE_RECONNECT_DELAY_MS * (1L << Math.min(attempt - 1, 4)),
                        MAX_RECONNECT_DELAY_MS
                );

                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    return;
                }

                if (manualClose) return;

                notifyReconnecting(attempt);
                System.out.println("CLIENT: reconnect attempt #" + attempt);

                try {
                    boolean success = reconnectBlocking();
                    if (success) {
                        System.out.println("CLIENT: reconnect succeeded");
                        reconnecting = false;
                        notifyReconnected();
                        return;
                    }
                } catch (InterruptedException e) {
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            reconnecting = false;
            System.out.println("CLIENT: reconnect failed after " + MAX_RECONNECT_ATTEMPTS + " attempts");
            notifyReconnectFailed();
        });
        reconnectThread.setDaemon(true);
        reconnectThread.start();
    }

    private void notifyReconnecting(int attempt) {
        if (listener instanceof GameMessageListener) {
            ((GameMessageListener) listener).onReconnecting(attempt);
        }
    }

    private void notifyReconnected() {
        if (listener instanceof GameMessageListener) {
            ((GameMessageListener) listener).onReconnected();
        }
    }

    private void notifyReconnectFailed() {
        if (listener instanceof GameMessageListener) {
            ((GameMessageListener) listener).onReconnectFailed();
        }
    }
}