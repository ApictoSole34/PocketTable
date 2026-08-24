package com.fizzycoyote.pockettable.network;

import com.fizzycoyote.pockettable.engine.poker.PokerGame;
import com.fizzycoyote.pockettable.engine.poker.PokerPlayer;
import com.fizzycoyote.pockettable.models.poker.PokerActionRequest;
import com.fizzycoyote.pockettable.models.poker.PokerGameState;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


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
public class PokerHostServer extends WebSocketServer {
    private static final int CONNECTION_LOST_TIMEOUT_SECONDS = 20;
    private static final int DISCONNECT_GRACE_PERIOD_SECONDS = 20;
    public static final String ERROR_PREFIX = "ERROR:";

    private final Set<WebSocket> clients = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<WebSocket, UUID> clientPlayerMap = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();
    private PokerGame game;
    private final UUID hostPlayerId;

    private final Map<UUID, ScheduledFuture<?>> pendingFolds = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public interface StateChangeListener {
        void onStateChanged(PokerGameState state);
    }
    private StateChangeListener stateListener;

    public PokerHostServer(int port, PokerGame game, UUID hostPlayerId) {
        super(new InetSocketAddress(port));
        this.game = game;
        this.hostPlayerId = hostPlayerId;
        setConnectionLostTimeout(CONNECTION_LOST_TIMEOUT_SECONDS);
    }

    public void setStateListener(StateChangeListener listener) {
        this.stateListener = listener;
    }

    private void notifyStateChanged() {
        if (stateListener != null) {
            PokerGameState state = PokerGameState.fromGame(game, hostPlayerId);
            stateListener.onStateChanged(state);
        }
    }

    public void setGame(PokerGame game) {
        this.game = game;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
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

        ScheduledFuture<?> pendingFold = pendingFolds.remove(playerId);
        if (pendingFold != null) {
            pendingFold.cancel(false);
            System.out.println("Player " + playerId + " reconnected before grace period expired - fold cancelled");
        }

        boolean exists = false;
        for (PokerPlayer p : game.getPlayers()) {
            if (p.getPlayerId().equals(playerId)) {
                p.setPlayerName(playerName);
                exists = true;
                break;
            }
        }

        if (!exists) {
            PokerPlayer newPlayer = new PokerPlayer(playerId, game.getStartingChips());
            newPlayer.setPlayerName(playerName);
            game.addPlayer(newPlayer);
            System.out.println("Player added: " + playerId + " (" + playerName + ")");
        }

        clientPlayerMap.put(conn, playerId);
        broadcastState();
        notifyStateChanged();
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("SOCKET CLOSED code=" + code + " reason=" + reason + " remote=" + remote);
        clients.remove(conn);
        UUID playerId = clientPlayerMap.remove(conn);

        if (playerId != null) {
            scheduleDelayedFold(playerId);
        }
    }

    /**
     * Instead of folding a disconnected player immediately, gives them a grace
     * period to reconnect (see {@link #onOpen}). If they don't reconnect in time,
     * the fold is applied and the game continues.
     */
    private void scheduleDelayedFold(UUID playerId) {
        if (pendingFolds.containsKey(playerId)) return;

        System.out.println("Player " + playerId + " disconnected - grace period " + DISCONNECT_GRACE_PERIOD_SECONDS + "s");

        ScheduledFuture<?> future = scheduler.schedule(() -> {
            pendingFolds.remove(playerId);

            boolean reconnected = clientPlayerMap.containsValue(playerId);
            if (reconnected) return;

            try {
                PokerPlayer player = game.getPlayer(playerId);
                if (!player.isFolded() && !game.isGameOver()) {
                    player.fold();
                    System.out.println("Player " + playerId + " folded (grace period expired)");
                    broadcastState();
                    notifyStateChanged();
                }
            } catch (Exception ignored) {}
        }, DISCONNECT_GRACE_PERIOD_SECONDS, TimeUnit.SECONDS);

        pendingFolds.put(playerId, future);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("HOST RECEIVED: " + message);
        try {
            if ("GET_STATE".equals(message)) {
                broadcastState();
                return;
            }
            PokerActionRequest request = gson.fromJson(message, PokerActionRequest.class);
            game.performAction(request.playerId(), request.action().name(), request.amount());
            broadcastState();
            notifyStateChanged();
        } catch (Exception e) {
            e.printStackTrace();
            conn.send(ERROR_PREFIX + e.getMessage());
        }
    }

    public void startNextHand() {
        game.resetForNewHand();
        broadcastGameStart();
    }

    public void broadcastState() {
        for (Map.Entry<WebSocket, UUID> entry : clientPlayerMap.entrySet()) {
            WebSocket conn = entry.getKey();
            UUID viewerId = entry.getValue();
            PokerGameState state = PokerGameState.fromGame(game, viewerId);
            GameMessage msg = new GameMessage(MessageType.STATE_UPDATE, state);
            conn.send(gson.toJson(msg));
        }
        notifyStateChanged();
    }

    public void broadcastGameStart() {
        for (Map.Entry<WebSocket, UUID> entry : clientPlayerMap.entrySet()) {
            WebSocket conn = entry.getKey();
            UUID viewerId = entry.getValue();
            PokerGameState state = PokerGameState.fromGame(game, viewerId);
            GameMessage msg = new GameMessage(MessageType.GAME_STARTED, state);
            conn.send(gson.toJson(msg));
        }
        notifyStateChanged();
    }

    @Override
    public void onError(WebSocket conn, Exception ex) { ex.printStackTrace(); }
    @Override
    public void onStart() { System.out.println("Server started on port " + getPort()); }

    public void stopServer() {
        try {
            for (ScheduledFuture<?> future : pendingFolds.values()) {
                future.cancel(false);
            }
            pendingFolds.clear();
            scheduler.shutdownNow();

            for (WebSocket c : clients) c.close();
            clients.clear();
            clientPlayerMap.clear();
            stop();
        } catch (Exception e) { e.printStackTrace(); }
    }
}