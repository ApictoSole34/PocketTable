package com.fizzycoyote.pockettable.lobby;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.view.ViewGroup;
import android.widget.Toast;

import com.fizzycoyote.pockettable.R;
import com.fizzycoyote.pockettable.engine.common.GameType;

import java.util.UUID;

public final class DevConnectionDialog {

    private DevConnectionDialog() {
    }

    public static void show(
            LobbyActivity activity,
            String currentPlayerName,
            GameType currentGameType
    ) {

        LinearLayout layout =
                new LinearLayout(activity);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setPadding(
                48,
                16,
                48,
                8
        );

        EditText etIp =
                new EditText(activity);

        etIp.setHint(
                "Host IP"
        );

        etIp.setSingleLine(true);

        etIp.setInputType(
                InputType.TYPE_CLASS_TEXT
        );

        layout.addView(
                etIp,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        EditText etRoomCode =
                new EditText(activity);

        etRoomCode.setHint(
                "Room Code"
        );

        etRoomCode.setSingleLine(true);

        etRoomCode.setInputType(
                InputType.TYPE_CLASS_TEXT
        );

        etRoomCode.setText(
                ""
        );

        layout.addView(
                etRoomCode,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        Spinner gameSpinner =
                new Spinner(activity);

        String[] games = {
                GameType.POKER.name(),
                GameType.COLOR_CLASH.name(),
                GameType.MAFIA.name()
        };

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        activity,
                        android.R.layout.simple_spinner_item,
                        games
                );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        gameSpinner.setAdapter(adapter);

        int selectedGame = 0;

        if (currentGameType == GameType.COLOR_CLASH) {
            selectedGame = 1;
        } else if (currentGameType == GameType.MAFIA) {
            selectedGame = 2;
        }

        gameSpinner.setSelection(
                selectedGame
        );

        layout.addView(
                gameSpinner,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        EditText etPlayerName =
                new EditText(activity);

        etPlayerName.setHint(
                "Player name"
        );

        etPlayerName.setSingleLine(true);

        etPlayerName.setText(
                currentPlayerName
        );

        layout.addView(
                etPlayerName,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        new AlertDialog.Builder(activity)
                .setTitle(
                        "DEV CONNECT"
                )
                .setMessage(
                        "Direct WebSocket connection"
                )
                .setView(layout)
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .setPositiveButton(
                        "Connect",
                        (dialog, which) -> {

                            String ip =
                                    etIp.getText()
                                            .toString()
                                            .trim();

                            String roomCode =
                                    etRoomCode
                                            .getText()
                                            .toString()
                                            .trim()
                                            .toUpperCase();

                            String playerName =
                                    etPlayerName
                                            .getText()
                                            .toString()
                                            .trim();

                            String selectedGameType =
                                    gameSpinner
                                            .getSelectedItem()
                                            .toString();

                            if (ip.isEmpty()) {

                                Toast.makeText(
                                        activity,
                                        "Enter host IP",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            if (roomCode.isEmpty()) {

                                Toast.makeText(
                                        activity,
                                        "Enter room code",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            if (playerName.isEmpty()) {

                                Toast.makeText(
                                        activity,
                                        "Enter player name",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            connect(
                                    activity,
                                    ip,
                                    roomCode,
                                    selectedGameType,
                                    playerName
                            );
                        }
                )
                .show();
    }

    private static void connect(
            LobbyActivity activity,
            String ip,
            String roomCode,
            String gameType,
            String playerName
    ) {

        Intent intent =
                new Intent(
                        activity,
                        LobbyActivity.class
                );

        intent.putExtra(
                "roomCode",
                roomCode
        );

        intent.putExtra(
                "playerId",
                UUID.randomUUID().toString()
        );

        intent.putExtra(
                "playerName",
                playerName
        );

        intent.putExtra(
                "isHost",
                false
        );

        intent.putExtra(
                "serverIp",
                ip
        );

        intent.putExtra(
                "gameType",
                gameType
        );

        intent.putExtra(
                "devConnection",
                true
        );

        activity.startActivity(intent);

        activity.finish();
    }
}