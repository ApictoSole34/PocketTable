package com.fizzycoyote.pockettable.network.common;

import com.google.gson.Gson;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Reusable WebSocket host server base for any local multiplayer game.
 *
 * <p>Handles connection lifecycle, ping/pong dead-connection detection, and a
 * disconnect grace period (so a player who briefly loses Wi-Fi isn't
 * immediately penalized). Subclasses implement the game-specific behavior:
 * what happens when a player joins, what happens if they don't reconnect in
 * time, and how to interpret incoming action messages.</p>
 *
 * <p><b>Room code enforcement:</b> every connecting client must send the
 * correct {@code roomCode} handshake header (see {@link GenericGameClient}).
 * Connections presenting a missing or mismatched code are rejected in
 * {@link #onOpen} before they're added to {@link #clientPlayerMap} or the
 * game is told about them. Without this check, the 6-character room code
 * only ever gated UDP discovery of the host's IP - anyone who reached the
 * IP directly (e.g. by scanning the local subnet) could join any game.</p>
 *
 * <p>Not tied to any particular game's rules or message format - each game
 * (poker, Color Clash, etc.) defines its own JSON payloads and passes them
 * through {@link #onClientMessage}.</p>
 */
public abstract class GenericHostServer extends WebSocketServer {

    public static final String ERROR_PREFIX = "ERROR:";

    private static final int CONNECTION_LOST_TIMEOUT_SECONDS = 20;
    private static final int DISCONNECT_GRACE_PERIOD_SECONDS = 20;
    private static final int INVALID_ROOM_CODE_CLOSE_CODE = 4001;

    private final String expectedRoomCode;

    private final Set<WebSocket> clients = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<WebSocket, UUID> clientPlayerMap = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledFuture<?>> pendingDisconnects = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    protected GenericHostServer(int port, String roomCode) {
        super(new InetSocketAddress(port));
        this.expectedRoomCode = roomCode;
        setConnectionLostTimeout(CONNECTION_LOST_TIMEOUT_SECONDS);
    }

    /**
     * Called when a player (re)connects. {@code isReconnect} is true if this
     * player was already known to the game (either still connected on another
     * socket, or returning within the grace period after a disconnect).
     */
    protected abstract void onPlayerJoined(UUID playerId, String playerName, boolean isReconnect);

    /**
     * Called when a player's grace period expires without them reconnecting.
     * The game should apply whatever penalty makes sense (e.g. auto-fold in
     * poker, auto-skip turn in a card game).
     */
    protected abstract void onPlayerDisconnectedPermanently(UUID playerId);

    /**
     * Called for every non-protocol message received from a client
     * (i.e. anything that isn't "GET_STATE"). The game is responsible for
     * parsing and applying it, and should call {@link #broadcastState()}
     * itself if the action changes state.
     */
    protected abstract void onClientMessage(UUID playerId, String message);

    /**
     * Called whenever the server needs to send a fresh state snapshot to a
     * specific viewer (e.g. after GET_STATE, after an action, or on demand).
     * Return the JSON string to send - typically a game-specific envelope
     * containing the serialized state.
     */
    protected abstract String buildStateMessage(UUID viewerId);

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        String incomingRoomCode = handshake.getFieldValue("roomCode");
        boolean roomCodeValid = expectedRoomCode == null
                || expectedRoomCode.isEmpty()
                || (incomingRoomCode != null && incomingRoomCode.equalsIgnoreCase(expectedRoomCode));

        if (!roomCodeValid) {
            System.out.println("REJECTED connection: room code mismatch (expected '"
                    + expectedRoomCode + "', got '" + incomingRoomCode + "')");
            conn.close(INVALID_ROOM_CODE_CLOSE_CODE, "Invalid room code");
            return;
        }

        clients.add(conn);

        String playerIdStr = handshake.getFieldValue("playerId");
        String playerName = handshake.getFieldValue("playerName");
        if (playerName == null || playerName.isEmpty()) playerName = "Player";

        UUID playerId;
        try {
            playerId = UUID.fromString(playerIdStr);
        } catch (Exception e) {
            playerId = UUID.randomUUID();
        }

        ScheduledFuture<?> pending = pendingDisconnects.remove(playerId);
        boolean isReconnect = pending != null;
        if (pending != null) {
            pending.cancel(false);
            System.out.println("Player " + playerId + " reconnected before grace period expired");
        }

        clientPlayerMap.put(conn, playerId);
        onPlayerJoined(playerId, playerName, isReconnect);
        broadcastState();
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("SOCKET CLOSED code=" + code + " reason=" + reason + " remote=" + remote);
        clients.remove(conn);
        UUID playerId = clientPlayerMap.remove(conn);

        if (playerId != null) {
            scheduleDelayedDisconnect(playerId);
        }
    }

    private void scheduleDelayedDisconnect(UUID playerId) {
        if (pendingDisconnects.containsKey(playerId)) return;

        System.out.println("Player " + playerId + " disconnected - grace period " + DISCONNECT_GRACE_PERIOD_SECONDS + "s");

        ScheduledFuture<?> future = scheduler.schedule(() -> {
            pendingDisconnects.remove(playerId);

            boolean reconnected = clientPlayerMap.containsValue(playerId);
            if (reconnected) return;

            onPlayerDisconnectedPermanently(playerId);
            broadcastState();
        }, DISCONNECT_GRACE_PERIOD_SECONDS, TimeUnit.SECONDS);

        pendingDisconnects.put(playerId, future);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("HOST RECEIVED: " + message);
        UUID playerId = clientPlayerMap.get(conn);
        if (playerId == null) return;

        try {
            if ("GET_STATE".equals(message)) {
                sendStateTo(conn, playerId);
                return;
            }
            onClientMessage(playerId, message);
        } catch (Exception e) {
            e.printStackTrace();
            conn.send(ERROR_PREFIX + e.getMessage());
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("Server started on port " + getPort());
    }

    private void sendStateTo(WebSocket conn, UUID viewerId) {
        String json = buildStateMessage(viewerId);
        conn.send(json);
    }

    /**
     * Sends a fresh state snapshot to every connected client, personalized
     * per viewer via {@link #buildStateMessage}. Call this after any action
     * that changes game state.
     */
    public void broadcastState() {
        System.out.println("BROADCAST: connected clients = " + clientPlayerMap.size());
        for (Map.Entry<WebSocket, UUID> entry : clientPlayerMap.entrySet()) {
            sendStateTo(entry.getKey(), entry.getValue());
        }
    }

    /** Sends a raw pre-built message to every connected client, unmodified. */
    protected void broadcastRaw(String json) {
        for (WebSocket conn : clients) {
            conn.send(json);
        }
    }

    public Map<WebSocket, UUID> getClientPlayerMap() {
        return Collections.unmodifiableMap(clientPlayerMap);
    }

    public void broadcastGameStartWithType(String gameType) {
        Map<String, String> payload = new HashMap<>();
        payload.put("gameType", gameType);
        GameMessage msg = new GameMessage(MessageType.GAME_STARTED, payload);
        String json = new Gson().toJson(msg);
        broadcastRaw(json);
    }

    /**
     * Tells every connected client the session is over (e.g. the host chose
     * to leave/end the game).
     */
    public void broadcastGameOver() {
        GameMessage msg = new GameMessage(MessageType.GAME_OVER, null);
        String json = new Gson().toJson(msg);
        broadcastRaw(json);
    }

    public void stopServer() {
        try {
            for (ScheduledFuture<?> future : pendingDisconnects.values()) {
                future.cancel(false);
            }
            pendingDisconnects.clear();
            scheduler.shutdownNow();

            for (WebSocket c : clients) c.close();
            clients.clear();
            clientPlayerMap.clear();
            stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}