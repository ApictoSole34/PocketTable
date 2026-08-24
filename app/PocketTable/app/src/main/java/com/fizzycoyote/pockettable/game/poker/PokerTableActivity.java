package com.fizzycoyote.pockettable.game.poker;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fizzycoyote.pockettable.R;
import com.fizzycoyote.pockettable.engine.common.Card;
import com.fizzycoyote.pockettable.engine.poker.PokerAction;
import com.fizzycoyote.pockettable.engine.poker.PokerGame;
import com.fizzycoyote.pockettable.engine.poker.PokerRound;
import com.fizzycoyote.pockettable.models.poker.PokerActionRequest;
import com.fizzycoyote.pockettable.models.poker.PokerGameState;
import com.fizzycoyote.pockettable.network.PokerClient;
import com.fizzycoyote.pockettable.network.PokerHostServer;
import com.fizzycoyote.pockettable.utils.ClientHolder;
import com.fizzycoyote.pockettable.utils.GameHolder;
import com.google.android.material.slider.Slider;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Main poker table screen.
 * Displays community cards, player's hand, chips, and action buttons.
 *
 * <p>Handles:
 * <ul>
 *   <li>UI updates from {@link PokerGameState}</li>
 *   <li>Player actions (CHECK, CALL, FOLD, BET, RAISE)</li>
 *   <li>Showdown winner display</li>
 *   <li>New hand (host only)</li>
 * </ul>
 * </p>
 */
public class PokerTableActivity extends AppCompatActivity {

    private TextView tvPot, tvTurnInfo, tvMyStack, tvMyBet;
    private ImageView[] communityViews = new ImageView[5];
    private ImageView myCard1, myCard2;
    private Button btnCheck, btnCall, btnFold, btnBet, btnRaise, btnNextHand;
    private RecyclerView rvTablePlayers;
    private TablePlayerAdapter tablePlayerAdapter;
    private PokerGameState lastState;

    private String roomCode;
    private UUID playerId;
    private String playerName;
    private boolean isHost;
    private String serverIp;

    private PokerHostServer hostServer;
    private PokerClient client;
    private PokerGame game;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poker_table);

        roomCode = getIntent().getStringExtra("roomCode");
        playerId = UUID.fromString(getIntent().getStringExtra("playerId"));
        playerName = getIntent().getStringExtra("playerName");
        if (playerName == null || playerName.isEmpty()) {
            playerName = getString(R.string.player_name_default);
        }
        isHost = getIntent().getBooleanExtra("isHost", false);
        serverIp = getIntent().getStringExtra("serverIp");

        tvPot = findViewById(R.id.tvPot);
        tvTurnInfo = findViewById(R.id.tvTurnInfo);
        tvMyStack = findViewById(R.id.tvMyStack);
        tvMyBet = findViewById(R.id.tvMyBet);
        communityViews[0] = findViewById(R.id.imgCommunity1);
        communityViews[1] = findViewById(R.id.imgCommunity2);
        communityViews[2] = findViewById(R.id.imgCommunity3);
        communityViews[3] = findViewById(R.id.imgCommunity4);
        communityViews[4] = findViewById(R.id.imgCommunity5);
        myCard1 = findViewById(R.id.imgMyCard1);
        myCard2 = findViewById(R.id.imgMyCard2);

        btnCheck = findViewById(R.id.btnCheck);
        btnCall = findViewById(R.id.btnCall);
        btnFold = findViewById(R.id.btnFold);
        btnBet = findViewById(R.id.btnBet);
        btnRaise = findViewById(R.id.btnRaise);
        btnNextHand = findViewById(R.id.btnNextHand);

        rvTablePlayers = findViewById(R.id.rvTablePlayers);
        rvTablePlayers.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        tablePlayerAdapter = new TablePlayerAdapter();
        rvTablePlayers.setAdapter(tablePlayerAdapter);

        btnCheck.setOnClickListener(v -> sendAction(PokerAction.CHECK, 0));
        btnCall.setOnClickListener(v -> sendAction(PokerAction.CALL, 0));
        btnFold.setOnClickListener(v -> sendAction(PokerAction.FOLD, 0));
        btnBet.setOnClickListener(v -> showBetDialog());
        btnRaise.setOnClickListener(v -> showRaiseDialog());
        btnNextHand.setOnClickListener(v -> startNextHand());

        if (isHost) {
            game = GameHolder.getInstance().getGame();
            hostServer = GameHolder.getInstance().getServer();
            if (game == null) {
                List<UUID> playerIds = new ArrayList<>();
                playerIds.add(playerId);
                game = new PokerGame(roomCode, playerIds);
                game.getPlayer(playerId).setPlayerName(playerName);
            }

            if (hostServer != null) {
                hostServer.setStateListener(state -> runOnUiThread(() -> updateUI(state)));
            }

            tvTurnInfo.setText(getString(R.string.you_are_host));
            PokerGameState initialState = PokerGameState.fromGame(game, playerId);
            updateUI(initialState);
        }

        if (!isHost && serverIp != null) {
            PokerClient existingClient = ClientHolder.getInstance().getClient();

            PokerClient.GameMessageListener listener = new PokerClient.GameMessageListener() {
                @Override
                public void onState(PokerGameState state) {
                    runOnUiThread(() -> updateUI(state));
                }

                @Override
                public void onGameStarted() {
                    runOnUiThread(() -> tvTurnInfo.setText(getString(R.string.game_started)));
                }

                @Override
                public void onGameOver() {
                    runOnUiThread(() -> {
                        tvTurnInfo.setText(getString(R.string.game_over));
                        enableButtons(false);
                    });
                }

                @Override
                public void onReconnecting(int attempt) {
                    runOnUiThread(() -> tvTurnInfo.setText(
                            getString(R.string.reconnecting_message, attempt)
                    ));
                }

                @Override
                public void onReconnected() {
                    runOnUiThread(() -> tvTurnInfo.setText(getString(R.string.reconnected_message)));
                }

                @Override
                public void onReconnectFailed() {
                    runOnUiThread(() -> {
                        tvTurnInfo.setText(getString(R.string.reconnect_failed_message));
                        enableButtons(false);
                    });
                }

                @Override
                public void onActionError(String message) {
                    runOnUiThread(() -> tvTurnInfo.setText(
                            getString(R.string.action_error_label, message)
                    ));
                }
            };

            if (existingClient != null) {
                client = existingClient;
                client.setListener(listener);
                client.send("GET_STATE");
                tvTurnInfo.setText(getString(R.string.connected));
            } else {
                try {
                    client = new PokerClient(new URI("ws://" + serverIp + ":8888"), playerId, playerName);
                    client.setListener(listener);
                    client.connect();
                    tvTurnInfo.setText(getString(R.string.connecting));
                } catch (Exception e) {
                    e.printStackTrace();
                    tvTurnInfo.setText(String.format(getString(R.string.connection_error), e.getMessage()));
                }
            }
        } else if (isHost) {
            PokerGameState initialState = PokerGameState.fromGame(game, playerId);
            updateUI(initialState);
            tvTurnInfo.setText(getString(R.string.you_are_host));
        }
    }

    private void onStateReceived(PokerGameState state) {
        runOnUiThread(() -> updateUI(state));
    }

    private void updateUI(PokerGameState state) {
        if (state == null) return;

        lastState = state;
        tvPot.setText(getString(R.string.pot_label) + state.totalPot());
        updateTablePlayers(state);

        PokerGameState.PlayerState myState = state.players().get(playerId);
        if (myState != null) {
            tvMyStack.setText(String.format(getString(R.string.your_chips_label), myState.chips()));
            tvMyBet.setText(String.format(getString(R.string.your_bet_label), myState.currentBet()));
        }

        List<Card> community = state.communityCards();
        for (int i = 0; i < 5; i++) {
            if (i < community.size()) {
                communityViews[i].setImageResource(getCardResource(community.get(i)));
            } else {
                communityViews[i].setImageResource(R.drawable.card_back);
            }
        }

        if (state.viewerId() != null && state.viewerId().equals(playerId)) {
            if (state.players().containsKey(playerId)) {
                List<Card> myHand = myState.hand();
                if (myHand != null && myHand.size() >= 2) {
                    myCard1.setImageResource(getCardResource(myHand.get(0)));
                    myCard2.setImageResource(getCardResource(myHand.get(1)));
                } else {
                    myCard1.setImageResource(R.drawable.card_back);
                    myCard2.setImageResource(R.drawable.card_back);
                }
            }
        }

        if (state.round() == PokerRound.SHOWDOWN) {
            UUID winnerId = state.winnerId();

            if (winnerId != null) {
                PokerGameState.PlayerState winnerState = state.players().get(winnerId);
                String winnerName = winnerState != null ? winnerState.playerName() : "Unknown";
                String handDesc = state.winnerHandDesc();
                if (handDesc == null) handDesc = getString(R.string.no_data);
                String displayHand = formatHandName(handDesc);
                tvTurnInfo.setText(String.format(getString(R.string.winner), winnerName) + "\n" + displayHand);
            } else {
                tvTurnInfo.setText(getString(R.string.tie));
            }
            enableButtons(false);
            btnNextHand.setVisibility(isHost ? View.VISIBLE : View.GONE);
            return;
        }

        UUID currentPlayer = state.currentPlayerId();
        if (currentPlayer != null && currentPlayer.equals(playerId)) {
            tvTurnInfo.setText(getString(R.string.your_turn));
            enableButtons(true);
        } else {
            String turnPlayerName = "?";
            if (currentPlayer != null && state.players().containsKey(currentPlayer)) {
                turnPlayerName = state.players().get(currentPlayer).playerName();
            }
            tvTurnInfo.setText(getString(R.string.waiting_for_move) + " (" + turnPlayerName + ")");
            enableButtons(false);
        }
        btnNextHand.setVisibility(View.GONE);
    }

    private void updateTablePlayers(PokerGameState state) {
        List<TablePlayerAdapter.Entry> entries = new ArrayList<>();
        for (PokerGameState.PlayerState p : state.players().values()) {
            boolean isDealer = state.dealerId() != null && state.dealerId().equals(p.playerId());
            boolean isCurrentTurn = state.currentPlayerId() != null && state.currentPlayerId().equals(p.playerId());
            boolean isMe = p.playerId().equals(playerId);
            entries.add(new TablePlayerAdapter.Entry(p, isDealer, isCurrentTurn, isMe));
        }
        tablePlayerAdapter.updateEntries(entries);
    }

    private void enableButtons(boolean enabled) {
        btnCheck.setEnabled(enabled);
        btnCall.setEnabled(enabled);
        btnFold.setEnabled(enabled);
        btnBet.setEnabled(enabled);
        btnRaise.setEnabled(enabled);
    }

    private void sendAction(PokerAction action, int amount) {
        if (isHost && game != null && hostServer != null) {
            try {
                game.performAction(playerId, action.name(), amount);
                hostServer.broadcastState();
                PokerGameState state = PokerGameState.fromGame(game, playerId);
                updateUI(state);
            } catch (Exception e) {
                tvTurnInfo.setText(String.format(getString(R.string.error_prefix), e.getMessage()));
                e.printStackTrace();
            }
        } else if (client != null) {
            PokerActionRequest request = new PokerActionRequest(playerId, action, amount);
            client.sendAction(request);
        } else {
            tvTurnInfo.setText(getString(R.string.no_connection));
        }
    }

    private PokerGameState.PlayerState getMyState() {
        if (lastState == null) return null;
        return lastState.players().get(playerId);
    }

    private void showBetDialog() {
        PokerGameState.PlayerState myState = getMyState();
        if (myState == null || myState.chips() <= 0) return;

        int min = 1;
        int max = myState.chips();
        showBetRaiseDialog(getString(R.string.bet_title), min, max, PokerAction.BET);
    }

    private void showRaiseDialog() {
        PokerGameState.PlayerState myState = getMyState();
        if (myState == null || lastState == null) return;

        int tableCurrentBet = lastState.currentBet();
        int minTotal = tableCurrentBet + 1;
        int maxTotal = myState.chips() + myState.currentBet();

        if (maxTotal < minTotal) {
            return;
        }

        showBetRaiseDialog(getString(R.string.raise_title), minTotal, maxTotal, PokerAction.RAISE);
    }

    private void showBetRaiseDialog(String title, int min, int max, PokerAction action) {
        int pot = lastState != null ? lastState.totalPot() : 0;

        if (max <= min) {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle(title);
            builder.setMessage(String.format(getString(R.string.all_in_only), max));
            builder.setPositiveButton(getString(R.string.all_in), (dialog, which) -> sendAction(action, max));
            builder.setNegativeButton(getString(R.string.cancel), null);
            builder.show();
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_bet_raise, null);
        TextView tvAmount = dialogView.findViewById(R.id.tvDialogAmount);
        Slider slider = dialogView.findViewById(R.id.sliderAmount);
        EditText etAmount = dialogView.findViewById(R.id.etAmount);
        Button btnHalfPot = dialogView.findViewById(R.id.btnHalfPot);
        Button btnPot = dialogView.findViewById(R.id.btnPot);
        Button btnAllIn = dialogView.findViewById(R.id.btnAllIn);

        slider.setValueFrom(min);
        slider.setValueTo(max);
        slider.setValue(min);

        final boolean[] updatingProgrammatically = {false};

        Runnable setInitial = () -> {
            updatingProgrammatically[0] = true;
            tvAmount.setText(String.valueOf(min));
            etAmount.setText(String.valueOf(min));
            updatingProgrammatically[0] = false;
        };
        setInitial.run();

        slider.addOnChangeListener((s, value, fromUser) -> {
            if (updatingProgrammatically[0]) return;
            int intValue = Math.round(value);
            updatingProgrammatically[0] = true;
            tvAmount.setText(String.valueOf(intValue));
            etAmount.setText(String.valueOf(intValue));
            updatingProgrammatically[0] = false;
        });

        etAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (updatingProgrammatically[0]) return;
                try {
                    int value = Integer.parseInt(s.toString());
                    int clamped = Math.max(min, Math.min(max, value));
                    updatingProgrammatically[0] = true;
                    slider.setValue(clamped);
                    tvAmount.setText(String.valueOf(clamped));
                    updatingProgrammatically[0] = false;
                } catch (NumberFormatException ignored) {}
            }
        });

        btnHalfPot.setOnClickListener(v -> {
            int value = Math.max(min, Math.min(max, pot / 2));
            updatingProgrammatically[0] = true;
            slider.setValue(value);
            tvAmount.setText(String.valueOf(value));
            etAmount.setText(String.valueOf(value));
            updatingProgrammatically[0] = false;
        });

        btnPot.setOnClickListener(v -> {
            int value = Math.max(min, Math.min(max, pot));
            updatingProgrammatically[0] = true;
            slider.setValue(value);
            tvAmount.setText(String.valueOf(value));
            etAmount.setText(String.valueOf(value));
            updatingProgrammatically[0] = false;
        });

        btnAllIn.setOnClickListener(v -> {
            updatingProgrammatically[0] = true;
            slider.setValue(max);
            tvAmount.setText(String.valueOf(max));
            etAmount.setText(String.valueOf(max));
            updatingProgrammatically[0] = false;
        });

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setView(dialogView);
        builder.setPositiveButton(getString(R.string.confirm), (dialog, which) -> {
            int finalAmount = Math.round(slider.getValue());
            sendAction(action, finalAmount);
        });
        builder.setNegativeButton(getString(R.string.cancel), null);
        builder.show();
    }

    private void startNextHand() {
        if (isHost && game != null && hostServer != null) {
            try {
                hostServer.startNextHand();
                PokerGameState state = PokerGameState.fromGame(game, playerId);
                updateUI(state);
            } catch (Exception e) {
                tvTurnInfo.setText(String.format(getString(R.string.error_prefix), e.getMessage()));
                e.printStackTrace();
            }
        }
    }

    private int getCardResource(Card card) {
        if (card == null) return R.drawable.card_back;
        String suit = card.suit().name().toLowerCase();
        String rank = card.rank().name().toLowerCase();

        rank = switch (rank) {
            case "two" -> "2";
            case "three" -> "3";
            case "four" -> "4";
            case "five" -> "5";
            case "six" -> "6";
            case "seven" -> "7";
            case "eight" -> "8";
            case "nine" -> "9";
            case "ten" -> "10";
            case "jack" -> "jack";
            case "queen" -> "queen";
            case "king" -> "king";
            case "ace" -> "ace";
            default -> rank;
        };

        String fileName = "card_" + rank + "_" + suit;
        int resourceId = getResources().getIdentifier(fileName, "drawable", getPackageName());
        return resourceId != 0 ? resourceId : R.drawable.card_back;
    }

    private String formatHandName(String rankName) {
        return switch (rankName) {
            case "HIGH_CARD" -> getString(R.string.hand_high_card);
            case "PAIR" -> getString(R.string.hand_pair);
            case "TWO_PAIR" -> getString(R.string.hand_two_pair);
            case "THREE_OF_A_KIND" -> getString(R.string.hand_three_of_a_kind);
            case "STRAIGHT" -> getString(R.string.hand_straight);
            case "FLUSH" -> getString(R.string.hand_flush);
            case "FULL_HOUSE" -> getString(R.string.hand_full_house);
            case "FOUR_OF_A_KIND" -> getString(R.string.hand_four_of_a_kind);
            case "STRAIGHT_FLUSH" -> getString(R.string.hand_straight_flush);
            default -> rankName;
        };
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (client != null) client.requestClose();
        ClientHolder.getInstance().clear();
    }
}