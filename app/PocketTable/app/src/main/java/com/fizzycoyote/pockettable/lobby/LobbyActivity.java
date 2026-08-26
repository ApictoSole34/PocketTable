package com.fizzycoyote.pockettable.lobby;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fizzycoyote.pockettable.R;
import com.fizzycoyote.pockettable.engine.colorclash.ColorClashGame;
import com.fizzycoyote.pockettable.engine.common.GameEngine;
import com.fizzycoyote.pockettable.engine.common.GameType;
import com.fizzycoyote.pockettable.engine.poker.PokerGame;
import com.fizzycoyote.pockettable.game.colorclash.ColorClashTableActivity;
import com.fizzycoyote.pockettable.game.poker.PokerTableActivity;
import com.fizzycoyote.pockettable.models.colorclash.ColorClashState;
import com.fizzycoyote.pockettable.models.poker.PokerGameState;
import com.fizzycoyote.pockettable.network.colorclash.ColorClashClient;
import com.fizzycoyote.pockettable.network.colorclash.ColorClashHostServer;
import com.fizzycoyote.pockettable.network.common.DiscoveryService;
import com.fizzycoyote.pockettable.network.common.GenericGameClient;
import com.fizzycoyote.pockettable.network.common.GenericHostServer;
import com.fizzycoyote.pockettable.network.poker.PokerClient;
import com.fizzycoyote.pockettable.network.poker.PokerHostServer;
import com.fizzycoyote.pockettable.utils.ClientHolder;
import com.fizzycoyote.pockettable.utils.GameHolder;
import com.fizzycoyote.pockettable.utils.NetworkUtils;
import com.fizzycoyote.pockettable.utils.RoomCodeGenerator;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Lobby screen where players join the game room.
 * Host can start the game and adjust game-specific settings (e.g. blinds for poker).
 * Client waits for host to start.
 *
 * <p>The host server is created and started as soon as the lobby opens (not
 * when START is clicked), so clients can connect and see the live player
 * list while waiting. Clicking START finalizes game settings and actually
 * begins the round.</p>
 *
 * <p><b>Important:</b> the client object created here is reused (via
 * {@link ClientHolder}) by the table activity after the game starts. It must
 * be a plain {@link PokerClient}/{@link ColorClashClient} - never wrap it in
 * an anonymous subclass overriding {@code onRawMessage}, since that would
 * permanently replace the client's own message-parsing logic and break the
 * table activity's ability to receive state updates after reuse.</p>
 *
 * @see PokerGame
 * @see PokerHostServer
 * @see PokerClient
 * @see ColorClashGame
 * @see ColorClashHostServer
 */
public class LobbyActivity extends AppCompatActivity {

    private TextView tvRoomCode, tvPlayerCount, tvIp;
    private RecyclerView rvPlayers;
    private Button btnStart;
    private PlayersAdapter adapter;
    private List<String> players = new ArrayList<>();

    private LinearLayout llGameSettings;
    private EditText etSmallBlind, etBigBlind, etStartingChips;

    private String roomCode;
    private UUID playerId;
    private String playerName;
    private boolean isHost;
    private String serverIp;

    private GenericHostServer hostServer;
    private GenericGameClient client;
    private GameEngine game;
    private boolean startingGame = false;

    private PokerGame pokerGame;
    private ColorClashGame colorClashGame;

    private GameType gameType;

    private static final String TAG = "LobbyActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);

        tvRoomCode = findViewById(R.id.tvRoomCode);
        tvIp = findViewById(R.id.tvHostIp);
        tvPlayerCount = findViewById(R.id.tvPlayerCount);
        rvPlayers = findViewById(R.id.rvPlayers);
        btnStart = findViewById(R.id.btnStart);
        Button btnShowQR = findViewById(R.id.btnShowQR);

        llGameSettings = findViewById(R.id.llGameSettings);
        etSmallBlind = findViewById(R.id.etSmallBlind);
        etBigBlind = findViewById(R.id.etBigBlind);
        etStartingChips = findViewById(R.id.etStartingChips);

        roomCode = getIntent().getStringExtra("roomCode");
        playerId = UUID.fromString(getIntent().getStringExtra("playerId"));
        playerName = getIntent().getStringExtra("playerName");
        if (playerName == null || playerName.isEmpty()) {
            playerName = getString(R.string.player_name_default);
        }
        isHost = getIntent().getBooleanExtra("isHost", false);
        serverIp = getIntent().getStringExtra("serverIp");

        String gameTypeStr = getIntent().getStringExtra("gameType");
        gameType = GameType.valueOf(gameTypeStr != null ? gameTypeStr : GameType.POKER.name());

        if (roomCode == null) {
            RoomCodeGenerator generator = new RoomCodeGenerator();
            roomCode = generator.generate();
        }

        tvRoomCode.setText(getString(R.string.code_label) + roomCode);

        adapter = new PlayersAdapter(players);
        rvPlayers.setLayoutManager(new LinearLayoutManager(this));
        rvPlayers.setAdapter(adapter);

        if (isHost) {
            String myIp = NetworkUtils.getLocalIpAddress();
            tvIp.setText(getString(R.string.ip_label) + myIp);
            tvIp.setVisibility(View.VISIBLE);

            DiscoveryService.broadcastHost(roomCode, myIp, gameType.name());

            if (gameType == GameType.POKER) {
                llGameSettings.setVisibility(View.VISIBLE);
            } else {
                llGameSettings.setVisibility(View.GONE);
            }

            btnStart.setVisibility(View.VISIBLE);
            btnShowQR.setVisibility(View.VISIBLE);
            btnShowQR.setOnClickListener(v -> showQRCode());
            btnStart.setOnClickListener(v -> startGame());

            setupHostServer();

            players.add(playerName + " (Host)");
            adapter.notifyDataSetChanged();
            tvPlayerCount.setText(getString(R.string.players_label) + "1");
        } else {
            tvIp.setVisibility(View.GONE);
            btnStart.setVisibility(View.GONE);
            llGameSettings.setVisibility(View.GONE);
            connectAsClient();
        }
    }

    /**
     * Creates the concrete client for this game type and connects.
     *
     * <p>Critical: this must be a plain {@code new PokerClient(...)} /
     * {@code new ColorClashClient(...)} - NOT wrapped in an anonymous
     * subclass overriding {@code onRawMessage}. Doing so would permanently
     * shadow the client's real message-parsing logic (which the table
     * activity depends on after reusing this same client via
     * {@link ClientHolder}), silently breaking all state updates.</p>
     */
    private void connectAsClient() {
        try {
            if (gameType == GameType.POKER) {
                PokerClient pokerClient = new PokerClient(
                        new URI("ws://" + serverIp + ":8888"), playerId, playerName);

                pokerClient.setListener(new PokerClient.GameMessageListener() {
                    @Override
                    public void onState(PokerGameState state) {
                        runOnUiThread(() -> updatePlayersFromPokerState(state));
                    }

                    @Override
                    public void onGameStarted() {
                        runOnUiThread(() -> startGameActivity(GameType.POKER.name()));
                    }

                    @Override public void onGameOver() {}
                    @Override public void onReconnecting(int attempt) {}
                    @Override public void onReconnected() {}
                    @Override public void onReconnectFailed() {}
                    @Override public void onActionError(String message) {}
                });

                pokerClient.connectBlocking();
                client = pokerClient;

            } else {
                ColorClashClient colorClient = new ColorClashClient(
                        new URI("ws://" + serverIp + ":8888"), playerId, playerName);

                colorClient.setListener(new ColorClashClient.MessageListener() {
                    @Override
                    public void onState(ColorClashState state) {
                        runOnUiThread(() -> updatePlayersFromColorClashState(state));
                    }

                    @Override
                    public void onGameStarted() {
                        runOnUiThread(() -> startGameActivity(GameType.COLOR_CLASH.name()));
                    }

                    @Override public void onGameOver() {}
                    @Override public void onReconnecting(int attempt) {}
                    @Override public void onReconnected() {}
                    @Override public void onReconnectFailed() {}
                    @Override public void onActionError(String message) {}
                });

                colorClient.connectBlocking();
                client = colorClient;
            }

            ClientHolder.getInstance().setClient(client);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Connection error", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupHostServer() {
        if (gameType == GameType.POKER) {
            pokerGame = new PokerGame(roomCode, List.of(playerId));
            pokerGame.getPlayer(playerId).setPlayerName(playerName);
            game = pokerGame;

            PokerHostServer server = new PokerHostServer(8888, pokerGame, playerId);
            server.setStateListener(state -> runOnUiThread(() -> updatePlayersFromPokerState(state)));
            hostServer = server;

        } else if (gameType == GameType.COLOR_CLASH) {
            colorClashGame = new ColorClashGame(List.of(playerId));
            colorClashGame.getPlayer(playerId).setPlayerName(playerName);
            game = colorClashGame;

            ColorClashHostServer server = new ColorClashHostServer(8888, colorClashGame, playerId);
            server.setStateListener(state -> runOnUiThread(() -> updatePlayersFromColorClashState(state)));
            hostServer = server;
        }

        try {
            hostServer.start();
            System.out.println("LOBBY HOST: server started");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updatePlayersFromPokerState(PokerGameState state) {
        if (state == null || state.players() == null) return;
        players.clear();
        for (PokerGameState.PlayerState p : state.players().values()) {
            String name = p.playerName();
            if (p.playerId().equals(playerId)) {
                name = name + " " + getString(R.string.you_suffix);
            }
            players.add(name);
        }
        adapter.notifyDataSetChanged();
        tvPlayerCount.setText(getString(R.string.players_label) + " " + players.size());
    }

    private void updatePlayersFromColorClashState(ColorClashState state) {
        if (state == null || state.players() == null) return;
        players.clear();
        for (ColorClashState.PlayerInfo p : state.players().values()) {
            String name = p.playerName();
            if (p.playerId().equals(playerId)) {
                name = name + " " + getString(R.string.you_suffix);
            }
            players.add(name);
        }
        adapter.notifyDataSetChanged();
        tvPlayerCount.setText(getString(R.string.players_label) + " " + players.size());
    }

    private void startGameActivity(String gameTypeStr) {
        if (startingGame) return;
        startingGame = true;

        Log.d(TAG, "startGameActivity: " + gameTypeStr);

        Intent intent;
        if (GameType.POKER.name().equals(gameTypeStr)) {
            intent = new Intent(this, PokerTableActivity.class);
        } else {
            intent = new Intent(this, ColorClashTableActivity.class);
        }
        intent.putExtra("roomCode", roomCode);
        intent.putExtra("playerId", playerId.toString());
        intent.putExtra("playerName", playerName);
        intent.putExtra("isHost", false);
        intent.putExtra("serverIp", serverIp);
        startActivity(intent);
        finish();
    }

    private void showQRCode() {
        String data = roomCode + ":" + NetworkUtils.getLocalIpAddress() + ":" + gameType.name();

        try {
            int size = 800;
            BitMatrix bitMatrix = new MultiFormatWriter().encode(data, BarcodeFormat.QR_CODE, size, size);
            Bitmap bitmap = Bitmap.createBitmap(bitMatrix.getWidth(), bitMatrix.getHeight(), Bitmap.Config.RGB_565);
            for (int x = 0; x < bitMatrix.getWidth(); x++) {
                for (int y = 0; y < bitMatrix.getHeight(); y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(48, 48, 48, 48);

            ImageView qrView = new ImageView(this);
            qrView.setImageBitmap(bitmap);
            qrView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            qrView.setLayoutParams(params);
            layout.addView(qrView);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.qr_title) + roomCode);
            builder.setView(layout);
            builder.setPositiveButton(getString(R.string.ok), null);
            builder.show();

        } catch (WriterException e) {
            e.printStackTrace();
            Toast.makeText(this, getString(R.string.qr_error), Toast.LENGTH_SHORT).show();
        }
    }

    private int parseIntOrDefault(String text, int defaultValue) {
        try {
            int value = Integer.parseInt(text.trim());
            return value > 0 ? value : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private void startGame() {
        startingGame = true;

        if (gameType == GameType.POKER) {
            int smallBlind = parseIntOrDefault(etSmallBlind.getText().toString(), 50);
            int bigBlind = parseIntOrDefault(etBigBlind.getText().toString(), 100);
            int startingChips = parseIntOrDefault(etStartingChips.getText().toString(), 1000);
            pokerGame.applySettings(smallBlind, bigBlind, startingChips);
        }

        GameHolder.getInstance().setGame(game, hostServer);
        game.startGame();
        hostServer.broadcastGameStartWithType(gameType.name());

        Intent intent = gameType == GameType.POKER
                ? new Intent(this, PokerTableActivity.class)
                : new Intent(this, ColorClashTableActivity.class);
        intent.putExtra("roomCode", roomCode);
        intent.putExtra("playerId", playerId.toString());
        intent.putExtra("playerName", playerName);
        intent.putExtra("isHost", true);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: startingGame=" + startingGame);
        if (!startingGame) {
            if (hostServer != null) {
                hostServer.stopServer();
            }
            if (client != null) {
                client.requestClose();
                ClientHolder.getInstance().clear();
            }
        }
    }
}