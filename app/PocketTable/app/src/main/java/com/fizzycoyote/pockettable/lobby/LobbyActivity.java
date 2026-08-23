package com.fizzycoyote.pockettable.lobby;


import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
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
import com.fizzycoyote.pockettable.engine.poker.PokerGame;
import com.fizzycoyote.pockettable.game.poker.PokerTableActivity;
import com.fizzycoyote.pockettable.models.poker.PokerGameState;
import com.fizzycoyote.pockettable.network.DiscoveryService;
import com.fizzycoyote.pockettable.network.PokerClient;
import com.fizzycoyote.pockettable.network.PokerHostServer;
import com.fizzycoyote.pockettable.utils.ClientHolder;
import com.fizzycoyote.pockettable.utils.GameHolder;
import com.fizzycoyote.pockettable.utils.NetworkUtils;
import com.fizzycoyote.pockettable.utils.RoomCodeGenerator;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.encoder.QRCode;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Lobby screen where players join the game room.
 * Host can start the game and adjust blinds/starting chips.
 * Client waits for host to start.
 *
 * <p>Communication:
 * <ul>
 *   <li>Host: Runs {@link PokerHostServer}</li>
 *   <li>Client: Connects via {@link PokerClient}</li>
 * </ul>
 * </p>
 *
 * @see PokerGame
 * @see PokerHostServer
 * @see PokerClient
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

    private PokerHostServer hostServer;
    private PokerClient client;
    private PokerGame game;
    private boolean startingGame = false;

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

            DiscoveryService.broadcastHost(roomCode, myIp);

            llGameSettings.setVisibility(View.VISIBLE);
            btnStart.setVisibility(View.VISIBLE);
            btnShowQR.setVisibility(View.VISIBLE);
            btnShowQR.setOnClickListener(v -> showQRCode());
            btnStart.setOnClickListener(v -> startGame());

            try {
                List<UUID> playerIds = new ArrayList<>();
                playerIds.add(playerId);
                game = new PokerGame(roomCode, playerIds);
                game.getPlayer(playerId).setPlayerName(playerName);
                hostServer = new PokerHostServer(8888, game, playerId);
                hostServer.start();

                hostServer.setStateListener(state -> runOnUiThread(() -> updatePlayersFromState(state)));
            } catch (Exception e) {
                e.printStackTrace();
            }

            players.add(playerName + " (Host)");
            adapter.notifyDataSetChanged();
            tvPlayerCount.setText(getString(R.string.players_label) + "1");
        } else {
            tvIp.setVisibility(View.GONE);
            btnStart.setVisibility(View.GONE);
            llGameSettings.setVisibility(View.GONE);

            try {
                client = new PokerClient(new URI("ws://" + serverIp + ":8888"), playerId, playerName);
                client.setListener(new PokerClient.GameMessageListener() {
                    @Override
                    public void onState(PokerGameState state) {
                        runOnUiThread(() -> updatePlayersFromState(state));
                    }

                    @Override
                    public void onGameStarted() {
                        runOnUiThread(() -> {
                            ClientHolder.getInstance().setClient(client);
                            client = null;

                            Intent intent = new Intent(LobbyActivity.this, PokerTableActivity.class);
                            intent.putExtra("roomCode", roomCode);
                            intent.putExtra("playerId", playerId.toString());
                            intent.putExtra("playerName", playerName);
                            intent.putExtra("isHost", false);
                            intent.putExtra("serverIp", serverIp);
                            startActivity(intent);
                            finish();
                        });
                    }

                    @Override
                    public void onGameOver() {}
                });
                client.connect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void showQRCode() {
        String data = roomCode + ":" + NetworkUtils.getLocalIpAddress();

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

    private void updatePlayersFromState(PokerGameState state) {
        players.clear();
        if (state != null && state.players() != null) {
            for (PokerGameState.PlayerState ps : state.players().values()) {
                String name = ps.playerName();
                if (name == null || name.isEmpty()) {
                    name = getString(R.string.player_name_default) + " " + ps.playerId().toString().substring(0, 4);
                }
                if (ps.playerId().equals(playerId)) {
                    name = name + " " + getString(R.string.you_suffix);
                }
                players.add(name);
            }
        }
        adapter.notifyDataSetChanged();
        tvPlayerCount.setText(getString(R.string.players_label) + " " + players.size());
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

        int smallBlind = parseIntOrDefault(etSmallBlind.getText().toString(), 50);
        int bigBlind = parseIntOrDefault(etBigBlind.getText().toString(), 100);
        int startingChips = parseIntOrDefault(etStartingChips.getText().toString(), 1000);

        game.applySettings(smallBlind, bigBlind, startingChips);

        GameHolder.getInstance().setGame(game, hostServer);
        game.startGame();
        hostServer.broadcastGameStart();

        Intent intent = new Intent(this, PokerTableActivity.class);
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
        if (!startingGame && hostServer != null) {
            hostServer.stopServer();
        }
        if (client != null) client.close();
    }
}