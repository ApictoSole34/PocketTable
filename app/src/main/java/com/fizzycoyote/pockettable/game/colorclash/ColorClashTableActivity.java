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

import androidx.activity.OnBackPressedCallback;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fizzycoyote.pockettable.BaseImmersiveActivity;
import com.fizzycoyote.pockettable.R;
import com.fizzycoyote.pockettable.engine.colorclash.CardColor;
import com.fizzycoyote.pockettable.engine.colorclash.CardType;
import com.fizzycoyote.pockettable.engine.colorclash.ColorClashCard;
import com.fizzycoyote.pockettable.engine.colorclash.ColorClashGame;
import com.fizzycoyote.pockettable.engine.colorclash.ColorClashRules;
import com.fizzycoyote.pockettable.models.colorclash.ColorClashState;
import com.fizzycoyote.pockettable.network.colorclash.ColorClashClient;
import com.fizzycoyote.pockettable.network.colorclash.ColorClashHostServer;
import com.fizzycoyote.pockettable.utils.AppDialog;
import com.fizzycoyote.pockettable.utils.ClientHolder;
import com.fizzycoyote.pockettable.utils.GameHolder;
import com.fizzycoyote.pockettable.utils.LeaveConfirmationHelper;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class ColorClashTableActivity extends BaseImmersiveActivity {

    private TextView tvDrawPile, tvTurnInfo;
    private ImageView imgTopCard, imgDrawPile;
    private RecyclerView rvHand;
    private Button btnDraw, btnNextRound, btnLastCard;
    private Button btnLeave;
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
            playerName = getString(R.string.player_name_default);
        }
        isHost = getIntent().getBooleanExtra("isHost", false);
        serverIp = getIntent().getStringExtra("serverIp");

        tvDrawPile = findViewById(R.id.tvDrawPile);
        tvTurnInfo = findViewById(R.id.tvTurnInfo);
        imgTopCard = findViewById(R.id.imgTopCard);
        imgDrawPile = findViewById(R.id.imgDrawPile);
        rvHand = findViewById(R.id.rvHand);
        btnDraw = findViewById(R.id.btnDraw);
        btnNextRound = findViewById(R.id.btnNextRound);
        btnLastCard = findViewById(R.id.btnLastCard);
        btnLeave = findViewById(R.id.btnLeave);
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

        applyTopInsetPadding(findViewById(R.id.llColorClashContent));
        btnLeave.setOnClickListener(v -> confirmLeave());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                confirmLeave();
            }
        });

        if (isHost) {
            game = (ColorClashGame) GameHolder.getInstance().getGame();
            hostServer = (ColorClashHostServer) GameHolder.getInstance().getServer();
            if (game == null) {
                List<UUID> playerIds = new ArrayList<>();
                playerIds.add(playerId);
                game = new ColorClashGame(this, playerIds);
                hostServer = new ColorClashHostServer(8888, game, playerId, roomCode);                try { hostServer.start(); } catch (Exception e) { e.printStackTrace(); }
            }
            hostServer.setStateListener(state -> runOnUiThread(() -> updateUI(state)));
            updateUI(ColorClashState.fromGame(game, playerId));
            tvTurnInfo.setText(getString(R.string.colorclash_host_ready));
        } else {
            ColorClashClient existingClient = (ColorClashClient) ClientHolder.getInstance().getClient();
            ColorClashClient.MessageListener listener = new ColorClashClient.MessageListener() {
                @Override public void onState(ColorClashState state) { runOnUiThread(() -> updateUI(state)); }
                @Override public void onGameStarted() { runOnUiThread(() -> tvTurnInfo.setText(getString(R.string.colorclash_game_started))); }
                @Override public void onGameOver() {
                    runOnUiThread(() -> {
                        Toast.makeText(ColorClashTableActivity.this,
                                getString(R.string.host_ended_game), Toast.LENGTH_LONG).show();
                        finish();
                    });
                }
                @Override public void onReconnecting(int attempt) {
                    runOnUiThread(() -> tvTurnInfo.setText(getString(R.string.reconnecting_message, attempt)));
                }
                @Override public void onReconnected() {
                    runOnUiThread(() -> tvTurnInfo.setText(getString(R.string.reconnected_message)));
                }
                @Override public void onReconnectFailed() {
                    runOnUiThread(() -> {
                        tvTurnInfo.setText(getString(R.string.reconnect_failed_message));
                        btnDraw.setEnabled(false);
                    });
                }
                @Override public void onActionError(String message) {
                    runOnUiThread(() -> Toast.makeText(ColorClashTableActivity.this,
                            getString(R.string.colorclash_error, message), Toast.LENGTH_SHORT).show());
                }
            };

            if (existingClient != null) {
                client = existingClient;
                client.setListener(listener);
                client.send("GET_STATE");
            } else {
                try {
                    client = new ColorClashClient(new URI("ws://" + serverIp + ":8888"), playerId, playerName, roomCode);                    client.setListener(listener);
                    client.connect();
                    tvTurnInfo.setText(getString(R.string.colorclash_connecting));
                } catch (Exception e) {
                    e.printStackTrace();
                    tvTurnInfo.setText(getString(R.string.colorclash_connection_error));
                }
            }
        }
    }

    private void confirmLeave() {
        int messageRes = isHost
                ? R.string.leave_confirm_message_host_game
                : R.string.leave_confirm_message_player;
        LeaveConfirmationHelper.show(this, messageRes, this::leaveGame);
    }

    private void leaveGame() {
        if (isHost && hostServer != null) {
            hostServer.broadcastGameOver();
        }
        finish();
    }

    /**
     * Updates the UI to reflect the current game state.
     *
     * <p>This method is called whenever a new state snapshot is received from the host
     * (or after the host processes an action locally). It updates:
     * <ul>
     *   <li>The top card and draw pile size</li>
     *   <li>The player's hand (via {@code handAdapter})</li>
     *   <li>The current turn indicator</li>
     *   <li>The opponents' bar with their card counts and status</li>
     *   <li>The "Last Card" button visibility</li>
     *   <li>The winner announcement and "Next Round" button (for the host)</li>
     * </ul>
     *
     * @param state the current game state snapshot, or {@code null} if no state is available
     *
     * @see #lastState
     * @see #handAdapter
     */
    private void updateUI(ColorClashState state) {
        if (state == null) return;
        lastState = state;

        ColorClashCard topCard = state.topCard();
        if (topCard != null) {
            imgTopCard.setImageResource(ColorClashCardResourceHelper.getCardResource(this, topCard));
        }
        tvDrawPile.setText(getString(R.string.colorclash_draw_pile_label, state.drawPileSize()));

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
            tvTurnInfo.setText(getString(R.string.your_turn));
            btnDraw.setEnabled(true);
        } else {
            tvTurnInfo.setText(getString(R.string.colorclash_waiting_for, currentName));
            btnDraw.setEnabled(false);
        }

        if (state.drawStack() > 0) {
            tvTurnInfo.append(getString(R.string.colorclash_draw_stack_info, state.drawStack()));
        }

        updateOpponentsBar(state);
        updateLastCardButton(state);

        if (state.winnerId() != null) {
            String winnerName = "?";
            if (state.players() != null && state.players().containsKey(state.winnerId())) {
                winnerName = state.players().get(state.winnerId()).playerName();
            }
            tvTurnInfo.setText(getString(R.string.colorclash_winner_message, winnerName));
            btnDraw.setEnabled(false);
            if (isHost) {
                btnNextRound.setVisibility(View.VISIBLE);
            }
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
            tvCount.setText(getResources().getQuantityString(
                    R.plurals.card_count, info.handSize(), info.handSize()));
            tvCount.setTextColor(isCurrentTurn ? Color.BLACK : Color.WHITE);
            tvCount.setGravity(Gravity.CENTER);
            chip.addView(tvCount);

            if (info.handSize() == 1 && !info.calledLastCard() && !info.eliminated()) {
                Button btnCatch = new Button(this);
                btnCatch.setText(getString(R.string.colorclash_catch_button));
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
        if (lastState == null || lastState.rules() == null) return false;
        return lastState.rules().canPlay(
                card,
                lastState.topCard(),
                lastState.currentColor(),
                lastState.drawStack()
        );
    }

    private boolean canJumpIn(ColorClashCard card) {
        if (lastState == null || lastState.rules() == null || !lastState.rules().jumpIn()) return false;
        if (lastState.drawStack() > 0) return false;
        ColorClashCard topCard = lastState.topCard();
        return topCard != null && !card.isWild() && card.equals(topCard);
    }

    private void showTargetChooser(Consumer<UUID> onTargetChosen) {
        if (lastState == null || lastState.players() == null) return;

        List<UUID> ids = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (var entry : lastState.players().entrySet()) {
            if (entry.getKey().equals(playerId)) continue;
            if (entry.getValue().eliminated()) continue;
            ids.add(entry.getKey());
            names.add(entry.getValue().playerName());
        }
        if (ids.isEmpty()) return;

        AlertDialog.Builder builder = AppDialog.builder(this);
        builder.setTitle(getString(R.string.colorclash_swap_cards_with));
        builder.setCancelable(false);
        builder.setItems(names.toArray(new String[0]), (dialog, which) -> onTargetChosen.accept(ids.get(which)));
        builder.show();
    }

    private void tryPlayCard(ColorClashCard card) {
        if (lastState == null) return;

        boolean myTurn = isMyTurn();
        boolean jumpingIn = !myTurn && canJumpIn(card);

        if (!myTurn && !jumpingIn) {
            Toast.makeText(this, getString(R.string.colorclash_not_your_turn), Toast.LENGTH_SHORT).show();
            return;
        }
        if (myTurn && !canPlayCard(card)) {
            Toast.makeText(this, getString(R.string.colorclash_cannot_play_card), Toast.LENGTH_SHORT).show();
            return;
        }

        List<ColorClashCard> hand = lastState.hands().get(playerId);
        if (hand == null) return;
        int index = hand.indexOf(card);
        if (index == -1) {
            Toast.makeText(this, getString(R.string.colorclash_card_not_in_hand), Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isSeven = card.type() == CardType.NUMBER && card.value() == 7;
        boolean sevenSwapActive = lastState.rules() != null && lastState.rules().sevenSwap();

        if (jumpingIn) {
            sendAction("JUMP_IN:" + index);
        } else if (card.isWild()) {
            showColorChooser(() -> {
                List<ColorClashCard> currentHand = lastState.hands().get(playerId);
                if (currentHand == null) return;
                int currentIndex = currentHand.indexOf(card);
                if (currentIndex == -1) {
                    Toast.makeText(this, getString(R.string.colorclash_card_no_longer_in_hand), Toast.LENGTH_SHORT).show();
                    return;
                }
                sendAction("PLAY:" + currentIndex + ":" + chosenColor.name());
            });
        } else if (isSeven && sevenSwapActive && hand.size() > 1) {
            showTargetChooser(targetId -> {
                List<ColorClashCard> currentHand = lastState.hands().get(playerId);
                if (currentHand == null) return;
                int currentIndex = currentHand.indexOf(card);
                if (currentIndex == -1) {
                    Toast.makeText(this, getString(R.string.colorclash_card_no_longer_in_hand), Toast.LENGTH_SHORT).show();
                    return;
                }
                sendAction("PLAY:" + currentIndex + ":TARGET:" + targetId);
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
        CardColor[] colorValues = {CardColor.RED, CardColor.YELLOW, CardColor.GREEN, CardColor.BLUE};
        String[] displayLabels = {
                getString(R.string.color_red),
                getString(R.string.color_yellow),
                getString(R.string.color_green),
                getString(R.string.color_blue)
        };
        AlertDialog.Builder builder = AppDialog.builder(this);
        builder.setTitle(getString(R.string.colorclash_choose_color));
        builder.setItems(displayLabels, (dialog, which) -> {
            chosenColor = colorValues[which];
            if (onColorChosen != null) onColorChosen.run();
        });
        builder.show();
    }

    private void drawCard() {
        sendAction("DRAW");
    }

    /**
     * Sends an action to the game engine.
     *
     * <p>If the player is the host, the action is executed locally and the state is broadcast
     * to all clients. If the player is a client, the action is sent to the host via WebSocket.
     *
     * <p>This method is safe to call from any thread; it runs the action on the appropriate thread.
     * The UI is updated automatically when the state changes.</p>
     *
     * @param action the action string in the format expected by {@link ColorClashGame#performAction}
     *
     * @see ColorClashGame#performAction(UUID, String, int)
     */
    private void sendAction(String action) {
        if (isHost && game != null) {
            try {
                game.performAction(playerId, action, 0);
                hostServer.broadcastState();
                updateUI(ColorClashState.fromGame(game, playerId));
            } catch (Exception e) {
                Toast.makeText(this, getString(R.string.colorclash_error, e.getMessage()), Toast.LENGTH_SHORT).show();
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
        GameHolder.getInstance().clear();
    }
}