package com.fizzycoyote.pockettable;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fizzycoyote.pockettable.engine.common.GameType;
import com.fizzycoyote.pockettable.lobby.LobbyActivity;
import com.fizzycoyote.pockettable.network.common.DiscoveryService;
import com.fizzycoyote.pockettable.utils.RoomCodeGenerator;
import com.fizzycoyote.pockettable.BuildConfig;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.UUID;

public class MainActivity extends BaseImmersiveActivity {

    private EditText etNickname;
    private Button btnJoinRoom, btnScanQR;
    private LinearLayout llPoker, llColorClash, llMafia;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etNickname = findViewById(R.id.etNickname);
        btnJoinRoom = findViewById(R.id.btnJoinRoom);
        btnScanQR = findViewById(R.id.btnScanQR);
        llPoker = findViewById(R.id.llPoker);
        llColorClash = findViewById(R.id.llColorClash);
        llMafia = findViewById(R.id.llMafia);

        String savedNick = getSharedPreferences("PocketTable", MODE_PRIVATE)
                .getString("nickname", "");
        if (!savedNick.isEmpty()) {
            etNickname.setText(savedNick);
        }

        llPoker.setOnClickListener(v -> startLobby(GameType.POKER));
        llColorClash.setOnClickListener(v -> startLobby(GameType.COLOR_CLASH));
        llMafia.setOnClickListener(v -> startLobby(GameType.MAFIA));

        btnJoinRoom.setOnClickListener(v -> {
            String nickname = etNickname.getText().toString().trim();
            if (nickname.isEmpty()) {
                Toast.makeText(this, getString(R.string.enter_nickname), Toast.LENGTH_SHORT).show();
                return;
            }
            saveNickname(nickname);
            showJoinDialog(nickname);
        });

        btnScanQR.setOnClickListener(v -> {
            String nickname = etNickname.getText().toString().trim();
            if (nickname.isEmpty()) {
                Toast.makeText(this, getString(R.string.enter_nickname), Toast.LENGTH_SHORT).show();
                return;
            }
            saveNickname(nickname);
            scanQR();
        });

        // ==================== DEV DEBUG =====================================
        Button btnDevConnect = findViewById(R.id.btnDevConnect);
        if (BuildConfig.DEBUG) {
            btnDevConnect.setVisibility(View.VISIBLE);
            btnDevConnect.setOnClickListener(v -> showDevConnectDialog());
        }
    }

    //============================================ DEV        ======================================================

    private void showDevConnectDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 16, 48, 8);

        // IP
        EditText etIp = new EditText(this);
        etIp.setHint("Host IP");
        etIp.setSingleLine(true);
        etIp.setInputType(InputType.TYPE_CLASS_TEXT);
        layout.addView(etIp);

        // Room Code
        EditText etRoomCode = new EditText(this);
        etRoomCode.setHint("Room Code");
        etRoomCode.setSingleLine(true);
        etRoomCode.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        layout.addView(etRoomCode);

        // Game Type
        Spinner spinnerGame = new Spinner(this);
        String[] gameTypes = {
                GameType.POKER.name(),
                GameType.COLOR_CLASH.name(),
                GameType.MAFIA.name()
        };
        ArrayAdapter<String> gameAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, gameTypes);
        gameAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGame.setAdapter(gameAdapter);
        layout.addView(spinnerGame);

        // Player Name
        EditText etPlayerName = new EditText(this);
        etPlayerName.setHint("Player name");
        etPlayerName.setSingleLine(true);
        etPlayerName.setText(etNickname.getText().toString().trim());
        layout.addView(etPlayerName);

        new android.app.AlertDialog.Builder(this)
                .setTitle("DEV CONNECT")
                .setMessage("Direct WebSocket connection")
                .setView(layout)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Connect", (dialog, which) -> {
                    String ip = etIp.getText().toString().trim();
                    String roomCode = etRoomCode.getText().toString().trim().toUpperCase();
                    String playerName = etPlayerName.getText().toString().trim();
                    String gameType = spinnerGame.getSelectedItem().toString();

                    if (ip.isEmpty()) {
                        Toast.makeText(this, "Enter host IP", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (roomCode.length() != 6) {
                        Toast.makeText(this, "Room code must contain 6 characters", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (playerName.isEmpty()) {
                        Toast.makeText(this, getString(R.string.enter_nickname), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    saveNickname(playerName);
                    connectDevClient(ip, roomCode, gameType, playerName);
                })
                .show();
    }

    private void connectDevClient(String serverIp, String roomCode, String gameType, String playerName) {
        UUID playerId = UUID.randomUUID();

        Intent intent = new Intent(MainActivity.this, LobbyActivity.class);
        intent.putExtra("roomCode", roomCode);
        intent.putExtra("playerId", playerId.toString());
        intent.putExtra("playerName", playerName);
        intent.putExtra("isHost", false);
        intent.putExtra("serverIp", serverIp);
        intent.putExtra("gameType", gameType);
        intent.putExtra("devConnection", true);

        Toast.makeText(this, "Connecting to " + serverIp + ":8888", Toast.LENGTH_SHORT).show();
        startActivity(intent);
    }

    //==============================================================================================================

    private void startLobby(GameType gameType) {
        String nickname = etNickname.getText().toString().trim();
        if (nickname.isEmpty()) {
            Toast.makeText(this, getString(R.string.enter_nickname), Toast.LENGTH_SHORT).show();
            return;
        }
        saveNickname(nickname);

        String roomCode = new RoomCodeGenerator().generate();
        UUID playerId = UUID.randomUUID();

        Intent intent = new Intent(this, LobbyActivity.class);
        intent.putExtra("roomCode", roomCode);
        intent.putExtra("playerId", playerId.toString());
        intent.putExtra("playerName", nickname);
        intent.putExtra("isHost", true);
        intent.putExtra("gameType", gameType.name());
        startActivity(intent);
    }

    private void saveNickname(String nickname) {
        getSharedPreferences("PocketTable", MODE_PRIVATE)
                .edit()
                .putString("nickname", nickname)
                .apply();
    }

    private void showJoinDialog(String nickname) {
        final String finalNickname = nickname;

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.join_title));

        final EditText input = new EditText(this);
        input.setHint(getString(R.string.join_hint));
        input.setMaxLines(1);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        builder.setView(input);

        builder.setPositiveButton(getString(R.string.join_search), (dialog, which) -> {
            String code = input.getText().toString().trim().toUpperCase();
            if (code.length() != 6) {
                Toast.makeText(this, getString(R.string.code_required), Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, getString(R.string.searching_host) + code, Toast.LENGTH_SHORT).show();

            UUID playerId = UUID.randomUUID();
            DiscoveryService.discoverHost(code, new DiscoveryService.DiscoveryListener() {
                @Override
                public void onHostFound(String ip, String roomCode, String gameType) {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, getString(R.string.host_found), Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(MainActivity.this, LobbyActivity.class);
                        intent.putExtra("roomCode", roomCode);
                        intent.putExtra("playerId", playerId.toString());
                        intent.putExtra("playerName", finalNickname);
                        intent.putExtra("isHost", false);
                        intent.putExtra("serverIp", ip);
                        intent.putExtra("gameType", gameType);
                        startActivity(intent);
                    });
                }

                @Override
                public void onHostNotFound() {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, getString(R.string.host_not_found) + code, Toast.LENGTH_LONG).show();
                    });
                }
            });
        });

        builder.setNegativeButton(getString(R.string.join_cancel), null);
        builder.show();
    }

    private void scanQR() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt(getString(R.string.scan_qr_from_host));
        integrator.setCameraId(0);
        integrator.setBeepEnabled(true);
        integrator.setBarcodeImageEnabled(true);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() != null) {
                String qrData = result.getContents();
                String[] parts = qrData.split(":");
                if (parts.length >= 3) {
                    String roomCode = parts[0];
                    String serverIp = parts[1];
                    String gameType = parts[2];

                    String nickname = etNickname.getText().toString().trim();
                    if (nickname.isEmpty()) nickname = getString(R.string.player_name_default);

                    UUID playerId = UUID.randomUUID();

                    Intent intent = new Intent(MainActivity.this, LobbyActivity.class);
                    intent.putExtra("roomCode", roomCode);
                    intent.putExtra("playerId", playerId.toString());
                    intent.putExtra("playerName", nickname);
                    intent.putExtra("isHost", false);
                    intent.putExtra("serverIp", serverIp);
                    intent.putExtra("gameType", gameType);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, getString(R.string.invalid_qr), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}