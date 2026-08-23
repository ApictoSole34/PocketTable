package com.fizzycoyote.pockettable;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;import android.os.Bundle;import android.widget.Button;import android.widget.EditText;import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.fizzycoyote.pockettable.lobby.LobbyActivity;
import com.fizzycoyote.pockettable.network.DiscoveryService;
import com.fizzycoyote.pockettable.utils.RoomCodeGenerator;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private EditText etNickname;
    private Button btnCreateRoom, btnJoinRoom, btnScanQR ;
    private SharedPreferences prefs;

    private void saveNickname(String nickname) {
        SharedPreferences prefs = getSharedPreferences("PocketTable", MODE_PRIVATE);
        prefs.edit().putString("nickname", nickname).apply();
    }

    private String loadNickname() {
        SharedPreferences prefs = getSharedPreferences("PocketTable", MODE_PRIVATE);
        return prefs.getString("nickname", "");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etNickname = findViewById(R.id.etNickname);
        btnCreateRoom = findViewById(R.id.btnCreateRoom);
        btnJoinRoom = findViewById(R.id.btnJoinRoom);
        btnScanQR = findViewById(R.id.btnScanQR);
        if (btnScanQR != null) {
            btnScanQR.setOnClickListener(v -> scanQR());
        }

        prefs = getSharedPreferences("PocketTablePrefs", Context.MODE_PRIVATE);
        String savedNick = loadNickname();
        if (!savedNick.isEmpty()) {
            etNickname.setText(savedNick);
        }

        btnCreateRoom.setOnClickListener(v -> {
            String nickname = etNickname.getText().toString().trim();
            if (nickname.isEmpty()) {
                Toast.makeText(this, "Enter your nickname!", Toast.LENGTH_SHORT).show();
                return;
            }
            saveNickname(nickname);

            RoomCodeGenerator generator = new RoomCodeGenerator();
            String roomCode = generator.generate();
            UUID playerId = UUID.randomUUID();

            Intent intent = new Intent(this, LobbyActivity.class);
            intent.putExtra("roomCode", roomCode);
            intent.putExtra("playerName", nickname);
            intent.putExtra("playerId", playerId.toString());
            intent.putExtra("isHost", true);
            startActivity(intent);
        });

        btnJoinRoom.setOnClickListener(v -> {
            String nickname = etNickname.getText().toString().trim();
            if (nickname.isEmpty()) {
                Toast.makeText(this, "Enter your nickname!", Toast.LENGTH_SHORT).show();
                return;
            }
            saveNickname(nickname);
            showJoinDialog(nickname);
        });
    }

    private void showJoinDialog(String nickname) {
        final String finalNickname = nickname;

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Join room");

        final EditText input = new EditText(this);
        input.setHint("Enter code (np. ABC123)");
        input.setMaxLines(1);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        builder.setView(input);

        builder.setPositiveButton("Search", (dialog, which) -> {
            String code = input.getText().toString().trim().toUpperCase();
            if (code.length() != 6) {
                Toast.makeText(this, "Code must be 6 characters!", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, "Searching for host with code: " + code, Toast.LENGTH_SHORT).show();

            UUID playerId = UUID.randomUUID();
            DiscoveryService.discoverHost(code, new DiscoveryService.DiscoveryListener() {
                @Override
                public void onHostFound(String ip, String roomCode) {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Host found!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(MainActivity.this, LobbyActivity.class);
                        intent.putExtra("roomCode", roomCode);
                        intent.putExtra("playerId", playerId.toString());
                        intent.putExtra("playerName", finalNickname);
                        intent.putExtra("isHost", false);
                        intent.putExtra("serverIp", ip);
                        startActivity(intent);
                    });
                }

                @Override
                public void onHostNotFound() {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Host not found for code: " + code, Toast.LENGTH_LONG).show();
                    });
                }
            });
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void scanQR() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("Scan QR from host screen");
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
                if (parts.length >= 2) {
                    String roomCode = parts[0];
                    String serverIp = parts[1];
                    String port = parts.length >= 3 ? parts[2] : "8888";

                    String nickname = etNickname.getText().toString().trim();
                    if (nickname.isEmpty()) nickname = "Gracz";

                    UUID playerId = UUID.randomUUID();

                    Intent intent = new Intent(MainActivity.this, LobbyActivity.class);
                    intent.putExtra("roomCode", roomCode);
                    intent.putExtra("playerId", playerId.toString());
                    intent.putExtra("playerName", nickname);
                    intent.putExtra("isHost", false);
                    intent.putExtra("serverIp", serverIp);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Incorrect QR", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
