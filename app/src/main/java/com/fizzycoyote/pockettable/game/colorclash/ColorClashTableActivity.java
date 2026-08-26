package com.fizzycoyote.pockettable.game.colorclash;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fizzycoyote.pockettable.R;
import com.fizzycoyote.pockettable.engine.colorclash.CardColor;
import com.fizzycoyote.pockettable.engine.colorclash.CardType;
import com.fizzycoyote.pockettable.engine.colorclash.ColorClashCard;
import com.fizzycoyote.pockettable.engine.colorclash.ColorClashGame;
import com.fizzycoyote.pockettable.engine.colorclash.ColorClashRules;
import com.fizzycoyote.pockettable.models.colorclash.ColorClashState;
import com.fizzycoyote.pockettable.network.colorclash.ColorClashClient;
import com.fizzycoyote.pockettable.network.colorclash.ColorClashHostServer;
import com.fizzycoyote.pockettable.utils.ClientHolder;
import com.fizzycoyote.pockettable.utils.GameHolder;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ColorClashTableActivity extends AppCompatActivity {

    private TextView tvTopCard, tvDrawPile, tvTurnInfo;
    private ImageView imgTopCard, imgDrawPile;
    private RecyclerView rvHand;
    private Button btnDraw, btnNextRound, btnLastCard;
    private LinearLayout llOpponents;

    private ColorClashHandAdapter handAdapter;
    private ColorClashState lastState;

    private String roomCode;
    private UUID playerId;
    private String playerName;
    private boolean isHost;
    private String serverIp;

    private ColorClashHostServer hostServer;
    private ColorClashClient client;
    private ColorClashGame game;

    private CardColor chosenColor = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_colorclash_table);

        roomCode = getIntent().getStringExtra("roomCode");
        playerId = UUID.fromString(getIntent().getStringExtra("playerId"));
        playerName = getIntent().getStringExtra("playerName");
        if (playerName == null || playerName.isEmpty()) {
            playerName = "Player";
        }
        isHost = getIntent().getBooleanExtra("isHost", false);
        serverIp = getIntent().getStringExtra("serverIp");

        tvTopCard = findViewById(R.id.tvTopCard);
        tvDrawPile = findViewById(R.id.tvDrawPile);
        tvTurnInfo = findViewById(R.id.tvTurnInfo);
        imgTopCard = findViewById(R.id.imgTopCard);
        imgDrawPile = findViewById(R.id.imgDrawPile);
        rvHand = findViewById(R.id.rvHand);
        btnDraw = findViewById(R.id.btnDraw);
        btnNextRound = findViewById(R.id.btnNextRound);
        btnLastCard = findViewById(R.id.btnLastCard);
        llOpponents = findViewById(R.id.llOpponents);

        imgDrawPile.setImageResource(R.drawable.card_back);

        rvHand.setLayoutManager(new GridLayoutManager(this, 3));
        handAdapter = new ColorClashHandAdapter(new ArrayList<>(), this::tryPlayCard);
        rvHand.setAdapter(handAdapter);

        btnDraw.setOnClickListener(v -> drawCard());
        btnLastCard.setOnClickListener(v -> sendAction("CALL_LAST_CARD"));

        btnNextRound.setOnClickListener(v -> {
            if (isHost && game != null) {
                game.resetForNewRound();
                hostServer.broadcastState();
                updateUI(ColorClashState.fromGame(game, playerId));
                btnNextRound.setVisibility(View.GONE);
            }
        });

        if (isHost) {
            game = (ColorClashGame) GameHolder.getInstance().getGame();
            hostServer = (ColorClashHostServer) GameHolder.getInstance().getServer();
            if (game == null) {
                List<UUID> playerIds = new ArrayList<>();
                playerIds.add(playerId);
                game = new ColorClashGame(playerIds);
                hostServer = new ColorClashHostServer(8888, game, playerId);
                try { hostServer.start(); } catch (Exception e) { e.printStackTrace(); }
            }
            hostServer.setStateListener(state -> runOnUiThread(() -> updateUI(state)));
            updateUI(ColorClashState.fromGame(game, playerId));
            tvTurnInfo.setText("You are the host. Game ready!");
        } else {
            ColorClashClient existingClient = (ColorClashClient) ClientHolder.getInstance().getClient();
            ColorClashClient.MessageListener listener = new ColorClashClient.MessageListener() {
                @Override public void onState(ColorClashState state) { runOnUiThread(() -> updateUI(state)); }
                @Override public void onGameStarted() { runOnUiThread(() -> tvTurnInfo.setText("Game started!")); }
                @Override public void onGameOver() { runOnUiThread(() -> tvTurnInfo.setText("Game over!")); }
                @Override public void onReconnecting(int attempt) {}
                @Override public void onReconnected() {}
                @Override public void onReconnectFailed() {}
                @Override public void onActionError(String message) {
                    runOnUiThread(() -> Toast.makeText(ColorClashTableActivity.this, message, Toast.LENGTH_SHORT).show());
                }
            };

            if (existingClient != null) {
                client = existingClient;
                client.setListener(listener);
                client.send("GET_STATE");
            } else {
                try {
                    client = new ColorClashClient(new URI("ws://" + serverIp + ":8888"), playerId, playerName);
                    client.setListener(listener);
                    client.connect();
                    tvTurnInfo.setText("Connecting...");
                } catch (Exception e) {
                    e.printStackTrace();
                    tvTurnInfo.setText("Connection error");
                }
            }
        }
    }

    private void updateUI(ColorClashState state) {
        if (state == null) return;
        lastState = state;

        ColorClashCard topCard = state.topCard();
        if (topCard != null) {
            tvTopCard.setText("Top: " + topCard.color() + " " + topCard.type());
            imgTopCard.setImageResource(ColorClashCardResourceHelper.getCardResource(this, topCard));
        }
        tvDrawPile.setText("Draw: " + state.drawPileSize());

        if (state.viewerId() != null && state.viewerId().equals(playerId) && state.hands() != null) {
            List<ColorClashCard> hand = state.hands().get(playerId);
            handAdapter.updateCards(hand != null ? hand : Collections.emptyList());
        }

        UUID current = state.currentPlayerId();
        String currentName = "...";
        if (current != null && state.players() != null && state.players().containsKey(current)) {
            currentName = state.players().get(current).playerName();
        }

        if (current != null && current.equals(playerId)) {
            tvTurnInfo.setText("YOUR TURN!");
            btnDraw.setEnabled(true);
        } else {
            tvTurnInfo.setText("Waiting for " + currentName);
            btnDraw.setEnabled(false);
        }

        if (state.drawStack() > 0) {
            tvTurnInfo.setText(tvTurnInfo.getText() + " (Draw " + state.drawStack() + " cards)");
        }

        updateOpponentsBar(state);
        updateLastCardButton(state);

        if (state.winnerId() != null) {
            String winnerName = "?";
            if (state.players() != null && state.players().containsKey(state.winnerId())) {
                winnerName = state.players().get(state.winnerId()).playerName();
            }
            tvTurnInfo.setText("\uD83C\uDFC6 " + winnerName + " WINS! \uD83C\uDFC6");
            btnDraw.setEnabled(false);
            if (isHost) {
                btnNextRound.setVisibility(View.VISIBLE);
            }
            btnDraw.setEnabled(false);
        } else {
            btnNextRound.setVisibility(View.GONE);
        }
    }

    private void updateOpponentsBar(ColorClashState state) {
        if (state.players() == null) return;
        llOpponents.removeAllViews();

        for (var entry : state.players().entrySet()) {
            UUID id = entry.getKey();
            if (id.equals(playerId)) continue;

            ColorClashState.PlayerInfo info = entry.getValue();
            boolean isCurrentTurn = id.equals(state.currentPlayerId());

            LinearLayout chip = new LinearLayout(this);
            chip.setOrientation(LinearLayout.VERTICAL);
            chip.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            chipParams.setMargins(8, 0, 8, 0);
            chip.setLayoutParams(chipParams);
            chip.setPadding(16, 8, 16, 8);
            chip.setBackgroundColor(isCurrentTurn ? Color.parseColor("#FFD700") : Color.parseColor("#2E5E3E"));

            TextView tvName = new TextView(this);
            String label = info.playerName() + (info.eliminated() ? " (out)" : "");
            tvName.setText(label);
            tvName.setTextColor(isCurrentTurn ? Color.BLACK : Color.WHITE);
            tvName.setTypeface(null, isCurrentTurn ? Typeface.BOLD : Typeface.NORMAL);
            chip.addView(tvName);

            TextView tvCount = new TextView(this);
            tvCount.setText(info.handSize() + " card" + (info.handSize() == 1 ? "" : "s"));
            tvCount.setTextColor(isCurrentTurn ? Color.BLACK : Color.WHITE);
            tvCount.setGravity(Gravity.CENTER);
            chip.addView(tvCount);

            if (info.handSize() == 1 && !info.calledLastCard() && !info.eliminated()) {
                Button btnCatch = new Button(this);
                btnCatch.setText("Catch!");
                btnCatch.setTextColor(Color.WHITE);
                btnCatch.setBackgroundColor(Color.parseColor("#D32F2F"));
                btnCatch.setOnClickListener(v -> sendAction("CATCH:" + id));
                chip.addView(btnCatch);
            }

            llOpponents.addView(chip);
        }
    }

    private void updateLastCardButton(ColorClashState state) {
        if (state.players() == null || !state.players().containsKey(playerId)) {
            btnLastCard.setVisibility(View.GONE);
            return;
        }
        ColorClashState.PlayerInfo me = state.players().get(playerId);
        boolean shouldShow = me.handSize() == 1 && !me.calledLastCard() && state.winnerId() == null;
        btnLastCard.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
    }

    private boolean canPlayCard(ColorClashCard card) {
        if (lastState == null) return false;
        return ColorClashRules.isPlayable(
                card,
                lastState.topCard(),
                lastState.currentColor(),
                lastState.drawStack()
        );
    }

    private void tryPlayCard(ColorClashCard card) {
        if (lastState == null) return;
        if (!isMyTurn()) {
            Toast.makeText(this, "It's not your turn!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!canPlayCard(card)) {
            Toast.makeText(this, "You cannot play this card", Toast.LENGTH_SHORT).show();
            return;
        }

        List<ColorClashCard> hand = lastState.hands().get(playerId);
        if (hand == null) return;
        int index = hand.indexOf(card);
        if (index == -1) {
            Toast.makeText(this, "Card not in hand", Toast.LENGTH_SHORT).show();
            return;
        }

        if (card.isWild()) {
            showColorChooser(() -> {
                List<ColorClashCard> currentHand = lastState.hands().get(playerId);
                if (currentHand == null) return;
                int currentIndex = currentHand.indexOf(card);
                if (currentIndex == -1) {
                    Toast.makeText(this, "Card no longer in hand", Toast.LENGTH_SHORT).show();
                    return;
                }
                sendAction("PLAY:" + currentIndex + ":" + chosenColor.name());
            });
        } else {
            sendAction("PLAY:" + index);
        }
    }

    private boolean isMyTurn() {
        if (lastState == null) return false;
        UUID current = lastState.currentPlayerId();
        return current != null && current.equals(playerId);
    }

    private void showColorChooser(Runnable onColorChosen) {
        String[] colors = {"RED", "YELLOW", "GREEN", "BLUE"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose color");
        builder.setItems(colors, (dialog, which) -> {
            chosenColor = CardColor.valueOf(colors[which]);
            if (onColorChosen != null) onColorChosen.run();
        });
        builder.show();
    }

    private void drawCard() {
        sendAction("DRAW");
    }

    private void sendAction(String action) {
        if (isHost && game != null) {
            try {
                game.performAction(playerId, action, 0);
                hostServer.broadcastState();
                updateUI(ColorClashState.fromGame(game, playerId));
            } catch (Exception e) {
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else if (client != null) {
            client.sendAction(action, 0);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (client != null) client.requestClose();
        ClientHolder.getInstance().clear();
        if (hostServer != null) hostServer.stopServer();
    }
}