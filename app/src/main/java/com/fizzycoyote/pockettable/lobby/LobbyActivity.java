package com.fizzycoyote.pockettable.lobby;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
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
import com.fizzycoyote.pockettable.engine.colorclash.ColorClashRules;
import com.fizzycoyote.pockettable.engine.common.GameEngine;
import com.fizzycoyote.pockettable.engine.common.GameType;
import com.fizzycoyote.pockettable.engine.mafia.MafiaGame;
import com.fizzycoyote.pockettable.engine.mafia.MafiaRole;
import com.fizzycoyote.pockettable.engine.mafia.MafiaRoleConfig;
import com.fizzycoyote.pockettable.engine.mafia.MafiaRules;
import com.fizzycoyote.pockettable.engine.mafia.TimedMafiaGame;
import com.fizzycoyote.pockettable.engine.poker.PokerGame;
import com.fizzycoyote.pockettable.game.colorclash.ColorClashTableActivity;
import com.fizzycoyote.pockettable.game.mafia.MafiaTableActivity;
import com.fizzycoyote.pockettable.game.poker.PokerTableActivity;
import com.fizzycoyote.pockettable.models.colorclash.ColorClashState;
import com.fizzycoyote.pockettable.models.mafia.MafiaState;
import com.fizzycoyote.pockettable.models.poker.PokerGameState;
import com.fizzycoyote.pockettable.network.colorclash.ColorClashClient;
import com.fizzycoyote.pockettable.network.colorclash.ColorClashHostServer;
import com.fizzycoyote.pockettable.network.common.DiscoveryService;
import com.fizzycoyote.pockettable.network.common.GenericGameClient;
import com.fizzycoyote.pockettable.network.common.GenericHostServer;
import com.fizzycoyote.pockettable.network.mafia.MafiaClient;
import com.fizzycoyote.pockettable.network.mafia.MafiaHostServer;
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
 * @see ColorClashClient
 * @see MafiaGame
 * @see TimedMafiaGame
 * @see MafiaClient
 * @see MafiaHostServer
 */
public class LobbyActivity extends AppCompatActivity {

    private TextView tvRoomCode, tvPlayerCount, tvIp;
    private RecyclerView rvPlayers;
    private Button btnStart;
    private PlayersAdapter adapter;
    private List<String> players = new ArrayList<>();

    // ====== poker ==============
    private LinearLayout llGameSettings;
    private EditText etSmallBlind, etBigBlind, etStartingChips;
    // ====== color clash ========
    private LinearLayout llColorClashSettings;
    private CheckBox cbStacking, cbJumpIn, cbSevenSwap, cbZeroRotate;
    // ====== mafia ==============
    private LinearLayout llMafiaSettings;
    private EditText etMafiaCount, etNeutralCount;
    private CheckBox cbDetective, cbDoctor, cbVigilante, cbMayor, cbJester, cbSerialKiller;
    private EditText etNightSeconds, etDaySeconds, etTrialSeconds;
    private CheckBox cbMafiaTimerEnabled;
    private MafiaGame mafiaGame;
    //===========================

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

        llColorClashSettings = findViewById(R.id.llColorClashSettings);
        cbStacking = findViewById(R.id.cbStacking);
        cbJumpIn = findViewById(R.id.cbJumpIn);
        cbSevenSwap = findViewById(R.id.cbSevenSwap);
        cbZeroRotate = findViewById(R.id.cbZeroRotate);

        llMafiaSettings = findViewById(R.id.llMafiaSettings);
        etMafiaCount = findViewById(R.id.etMafiaCount);
        etNeutralCount = findViewById(R.id.etNeutralCount);
        cbDetective = findViewById(R.id.cbDetective);
        cbDoctor = findViewById(R.id.cbDoctor);
        cbVigilante = findViewById(R.id.cbVigilante);
        cbMayor = findViewById(R.id.cbMayor);
        cbJester = findViewById(R.id.cbJester);
        cbSerialKiller = findViewById(R.id.cbSerialKiller);
        etNightSeconds = findViewById(R.id.etNightSeconds);
        etDaySeconds = findViewById(R.id.etDaySeconds);
        etTrialSeconds = findViewById(R.id.etTrialSeconds);
        cbMafiaTimerEnabled = findViewById(R.id.cbMafiaTimerEnabled);

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
            } else if (gameType == GameType.COLOR_CLASH) {
                llGameSettings.setVisibility(View.GONE);
                llColorClashSettings.setVisibility(View.VISIBLE);
            }  else if (gameType == GameType.MAFIA) {
                llGameSettings.setVisibility(View.GONE);
                llColorClashSettings.setVisibility(View.GONE);
                llMafiaSettings.setVisibility(View.VISIBLE);
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

            } else if (gameType == GameType.COLOR_CLASH){
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
            } else if (gameType == GameType.MAFIA) {
                MafiaClient mafiaClient = new MafiaClient(new URI("ws://" + serverIp + ":8888"), playerId, playerName);
                mafiaClient.setListener(new MafiaClient.MessageListener() {
                    @Override public void onState(MafiaState state) { runOnUiThread(() -> updatePlayersFromMafiaState(state)); }
                    @Override public void onGameStarted() { runOnUiThread(() -> startGameActivity(GameType.MAFIA.name())); }
                    @Override public void onGameOver() {}
                    @Override public void onReconnecting(int attempt) {}
                    @Override public void onReconnected() {}
                    @Override public void onReconnectFailed() {}
                    @Override public void onActionError(String message) {}
                });
                mafiaClient.connectBlocking();
                client = mafiaClient;
            }

            ClientHolder.getInstance().setClient(client);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, getString(R.string.lobby_con_error), Toast.LENGTH_SHORT).show();
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
        }   else if (gameType == GameType.MAFIA) {
            mafiaGame = new TimedMafiaGame(this, List.of(playerId));
            mafiaGame.getPlayer(playerId).setPlayerName(playerName);
            game = mafiaGame;

            MafiaHostServer server = new MafiaHostServer(8888, mafiaGame, playerId);
            server.setStateListener(state -> runOnUiThread(() -> updatePlayersFromMafiaState(state)));
            hostServer = server;
        }

        try {
            hostServer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updatePlayersFromMafiaState(MafiaState state) {
        if (state == null || state.players() == null) return;
        players.clear();
        for (MafiaState.PlayerInfo p : state.players().values()) {
            String name = p.playerName();
            if (p.playerId().equals(playerId)) name += getString(R.string.lobby_you);
            players.add(name);
        }
        adapter.notifyDataSetChanged();
        tvPlayerCount.setText(getString(R.string.players_label) + players.size());
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
        } else if (GameType.COLOR_CLASH.name().equals(gameTypeStr)) {
            intent = new Intent(this, ColorClashTableActivity.class);
        } else {
            intent = new Intent(this, MafiaTableActivity.class);
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
        if (gameType == GameType.POKER) {
            int smallBlind = parseIntOrDefault(etSmallBlind.getText().toString(), 50);
            int bigBlind = parseIntOrDefault(etBigBlind.getText().toString(), 100);
            int startingChips = parseIntOrDefault(etStartingChips.getText().toString(), 1000);
            pokerGame.applySettings(smallBlind, bigBlind, startingChips);

        } else if (gameType == GameType.COLOR_CLASH) {
            boolean stacking = cbStacking.isChecked();
            boolean jumpIn = cbJumpIn.isChecked();
            boolean sevenSwap = cbSevenSwap.isChecked();
            boolean zeroRotate = cbZeroRotate.isChecked();
            ColorClashRules rules = new ColorClashRules(stacking, jumpIn, sevenSwap, zeroRotate);
            if (colorClashGame != null) {
                colorClashGame.setRules(rules);
            }

        } else if (gameType == GameType.MAFIA) {
            int mafiaCount = parseIntOrDefault(etMafiaCount.getText().toString(), 1);
            boolean hasDetective = cbDetective.isChecked();
            boolean hasDoctor = cbDoctor.isChecked();
            boolean hasVigilante = cbVigilante.isChecked();
            boolean hasMayor = cbMayor.isChecked();
            int neutralCount = parseIntOrDefault(etNeutralCount.getText().toString(), 0);
            boolean hasJester = cbJester.isChecked();
            boolean hasSerialKiller = cbSerialKiller.isChecked();

            int allowedNeutralClasses = (hasJester ? 1 : 0) + (hasSerialKiller ? 1 : 0);
            if (neutralCount > allowedNeutralClasses) {
                Toast.makeText(this,
                        getString(R.string.lobby_toast_mafia_neutral),
                        Toast.LENGTH_SHORT).show();
                return;
            }

            MafiaRoleConfig cfg = new MafiaRoleConfig(this);
            cfg.setCount(MafiaRole.MAFIA, mafiaCount);
            cfg.setCount(MafiaRole.DETECTIVE, hasDetective ? 1 : 0);
            cfg.setCount(MafiaRole.DOCTOR, hasDoctor ? 1 : 0);
            cfg.setCount(MafiaRole.VIGILANTE, hasVigilante ? 1 : 0);
            cfg.setCount(MafiaRole.MAYOR, hasMayor ? 1 : 0);

            cfg.setNeutralCount(neutralCount);
            List<MafiaRole> allowedNeutrals = new ArrayList<>();
            if (hasJester) allowedNeutrals.add(MafiaRole.JESTER);
            if (hasSerialKiller) allowedNeutrals.add(MafiaRole.SERIAL_KILLER);
            cfg.setAllowedNeutralRoles(allowedNeutrals);

            try {
                cfg.validateForPlayerCount(mafiaGame.getPlayers().size());
            } catch (IllegalStateException e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }

            int night = parseIntOrDefault(etNightSeconds.getText().toString(), 45);
            int day = parseIntOrDefault(etDaySeconds.getText().toString(), 90);
            int trial = parseIntOrDefault(etTrialSeconds.getText().toString(), 30);
            boolean timer = cbMafiaTimerEnabled.isChecked();

            mafiaGame.setRoleConfig(cfg);
            mafiaGame.setRules(new MafiaRules(timer, night, day, trial));
        }

        GameHolder.getInstance().clear();

        try {
            game.startGame();
        } catch (IllegalStateException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        startingGame = true;
        GameHolder.getInstance().setGame(game, hostServer);
        hostServer.broadcastGameStartWithType(gameType.name());

        Intent intent;
        if (gameType == GameType.POKER) {
            intent = new Intent(this, PokerTableActivity.class);
        } else if (gameType == GameType.COLOR_CLASH) {
            intent = new Intent(this, ColorClashTableActivity.class);
        } else {
            intent = new Intent(this, MafiaTableActivity.class);
        }
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