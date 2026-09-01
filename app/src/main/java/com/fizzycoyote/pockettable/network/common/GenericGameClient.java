package com.fizzycoyote.pockettable.network.common;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Reusable WebSocket client base for any local multiplayer game.
 *
 * <p>Handles ping/pong dead-connection detection and automatic reconnect
 * with exponential backoff when the connection drops unexpectedly (as
 * opposed to being closed intentionally via {@link #requestClose()}).</p>
 *
 * <p>The room code is sent as a handshake header alongside the player's
 * identity. The host validates it in {@link GenericHostServer#onOpen} and
 * rejects the connection if it doesn't match - without this, anyone who
 * could reach the host's IP on the local network could join a game without
 * ever knowing its room code, since the code was previously used only for
 * UDP discovery, never checked at the WebSocket layer.</p>
 *
 * <p>Subclasses interpret incoming raw messages according to their game's
 * own JSON format via {@link #onRawMessage}.</p>
 */
public abstract class GenericGameClient extends WebSocketClient {

    private static final int CONNECTION_LOST_TIMEOUT_SECONDS = 20;
    private static final int MAX_RECONNECT_ATTEMPTS = 10;
    private static final long BASE_RECONNECT_DELAY_MS = 2000;
    private static final long MAX_RECONNECT_DELAY_MS = 15000;

    private volatile boolean manualClose = false;
    private volatile boolean reconnecting = false;
    private Thread reconnectThread;

    protected GenericGameClient(URI uri, UUID playerId, String playerName, String roomCode) {
        super(uri, buildHeaders(playerId, playerName, roomCode));
        setConnectionLostTimeout(CONNECTION_LOST_TIMEOUT_SECONDS);
    }

    private static Map<String, String> buildHeaders(UUID playerId, String playerName, String roomCode) {
        Map<String, String> headers = new HashMap<>();
        headers.put("playerId", playerId.toString());
        headers.put("playerName", playerName != null ? playerName : "Player");
        headers.put("roomCode", roomCode != null ? roomCode : "");
        return headers;
    }

    /** Called for every incoming message, including the error prefix - the subclass decides how to interpret it. */
    protected abstract void onRawMessage(String message);

    /** Called when a reconnect attempt is about to be made. */
    protected void onReconnecting(int attempt) {}

    /** Called when reconnect succeeds. */
    protected void onReconnected() {}

    /** Called when all reconnect attempts have been exhausted. */
    protected void onReconnectFailed() {}

    @Override
    public void onOpen(ServerHandshake handshake) {
        send("GET_STATE");
    }

    @Override
    public void onMessage(String message) {
        onRawMessage(message);
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

    /** Call this when the player intentionally leaves the game (prevents auto-reconnect). */
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

                onReconnecting(attempt);
                System.out.println("CLIENT: reconnect attempt #" + attempt);

                try {
                    if (reconnectBlocking()) {
                        System.out.println("CLIENT: reconnect succeeded");
                        reconnecting = false;
                        onReconnected();
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
            onReconnectFailed();
        });
        reconnectThread.setDaemon(true);
        reconnectThread.start();
    }
}