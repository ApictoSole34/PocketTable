package com.fizzycoyote.pockettable.game.mafia;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fizzycoyote.pockettable.R;
import com.fizzycoyote.pockettable.engine.mafia.MafiaGame;
import com.fizzycoyote.pockettable.engine.mafia.MafiaPhase;
import com.fizzycoyote.pockettable.engine.mafia.MafiaRole;
import com.fizzycoyote.pockettable.models.mafia.MafiaState;
import com.fizzycoyote.pockettable.network.mafia.MafiaClient;
import com.fizzycoyote.pockettable.network.mafia.MafiaHostServer;
import com.fizzycoyote.pockettable.utils.ClientHolder;
import com.fizzycoyote.pockettable.utils.GameHolder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MafiaTableActivity extends AppCompatActivity {

    private static final int GRID_COLUMNS = 3;

    private TextView tvPhase, tvRole, tvInfo, tvTimer, tvCandidateName;
    private TextView tvGuiltyCount, tvNotGuiltyCount;
    private View candidatePanel;
    private RecyclerView rvPlayers;
    private Button btnSkip, btnRoleInfo, btnNotes;
    private ImageButton btnGuilty, btnNotGuilty;
    private MafiaPlayerAdapter adapter;

    private String roomCode;
    private UUID playerId;
    private String playerName;
    private boolean isHost;
    private String serverIp;

    private MafiaHostServer hostServer;
    private MafiaClient client;
    private MafiaGame game;
    private MafiaState lastState;

    private CountDownTimer localTimer;

    private boolean firstStateReceived = false;
    private int lastNightDialogDay = -1;
    private int lastDayEliminationDialogDay = -1;
    private int lastInvestigationDialogDay = -1;
    private boolean winDialogShown = false;

    private AlertDialog waitingForHostDialog;
    private MafiaPhase previousPhase = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mafia_table);

        tvPhase = findViewById(R.id.tvPhase);
        tvRole = findViewById(R.id.tvRole);
        tvInfo = findViewById(R.id.tvInfo);
        tvTimer = findViewById(R.id.tvTimer);
        rvPlayers = findViewById(R.id.rvPlayers);
        btnSkip = findViewById(R.id.btnSkip);
        candidatePanel = findViewById(R.id.candidatePanel);
        tvCandidateName = findViewById(R.id.tvCandidateName);
        btnGuilty = findViewById(R.id.btnGuilty);
        btnNotGuilty = findViewById(R.id.btnNotGuilty);
        tvGuiltyCount = findViewById(R.id.tvGuiltyCount);
        tvNotGuiltyCount = findViewById(R.id.tvNotGuiltyCount);
        btnRoleInfo = findViewById(R.id.btnRoleInfo);
        btnRoleInfo.setOnClickListener(v -> RoleInfoHelper.show(this));
        btnNotes = findViewById(R.id.btnNotes);
        btnNotes.setOnClickListener(v -> showNotesDialog());

        roomCode = getIntent().getStringExtra("roomCode");
        playerId = UUID.fromString(getIntent().getStringExtra("playerId"));
        playerName = getIntent().getStringExtra("playerName");
        isHost = getIntent().getBooleanExtra("isHost", false);
        serverIp = getIntent().getStringExtra("serverIp");

        adapter = new MafiaPlayerAdapter(new ArrayList<>(), this::onPlayerClick);
        rvPlayers.setLayoutManager(new GridLayoutManager(this, GRID_COLUMNS));
        rvPlayers.setAdapter(adapter);

        btnSkip.setOnClickListener(v -> {
            if (lastState == null) return;
            switch (lastState.phase()) {
                case NIGHT:
                    sendAction("NIGHT_PASS");
                    break;
                case DAY_NOMINATION:
                    sendAction("DAY_NOMINATE:SKIP");
                    break;
                case DAY_VOTE:
                    sendAction("DAY_VOTE:NO");
                    break;
                default:
            }
        });

        btnGuilty.setOnClickListener(v -> {
            if (lastState != null && lastState.phase() == MafiaPhase.DAY_VOTE) {
                sendAction("DAY_VOTE:YES");
            }
        });
        btnNotGuilty.setOnClickListener(v -> {
            if (lastState != null && lastState.phase() == MafiaPhase.DAY_VOTE) {
                sendAction("DAY_VOTE:NO");
            }
        });

        if (isHost) {
            game = (MafiaGame) GameHolder.getInstance().getGame();
            hostServer = (MafiaHostServer) GameHolder.getInstance().getServer();
            if (hostServer != null) {
                hostServer.setStateListener(state -> runOnUiThread(() -> updateUI(state)));
            }
            updateUI(snapshotFor(playerId));
        } else {
            MafiaClient existing = (MafiaClient) ClientHolder.getInstance().getClient();
            if (existing != null) {
                client = existing;
                client.setListener(createListener());
                client.send("GET_STATE");
            } else {
                try {
                    client = new MafiaClient(new URI("ws://" + serverIp + ":8888"), playerId, playerName);
                    client.setListener(createListener());
                    client.connect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private MafiaState snapshotFor(UUID viewerId) {
        return (MafiaState) game.getState(viewerId);
    }

    private MafiaClient.MessageListener createListener() {
        return new MafiaClient.MessageListener() {
            @Override public void onState(MafiaState state) { runOnUiThread(() -> updateUI(state)); }
            @Override public void onGameStarted() {
                runOnUiThread(() -> tvInfo.setText(R.string.mafia_game_started));
            }
            @Override public void onGameOver() {
                runOnUiThread(() -> {
                    if (waitingForHostDialog != null && waitingForHostDialog.isShowing()) {
                        waitingForHostDialog.dismiss();
                    }
                    Toast.makeText(MafiaTableActivity.this, R.string.mafia_host_ended_game, Toast.LENGTH_LONG).show();
                    finish();
                });
            }
            @Override public void onReconnecting(int attempt) {}
            @Override public void onReconnected() {}
            @Override public void onReconnectFailed() {}
            @Override public void onActionError(String message) {
                runOnUiThread(() -> Toast.makeText(MafiaTableActivity.this, message, Toast.LENGTH_SHORT).show());
            }
        };
    }

    private void startLocalTimer(int seconds) {
        if (localTimer != null) {
            localTimer.cancel();
        }

        if (seconds <= 0) {
            tvTimer.setVisibility(View.GONE);
            return;
        }

        tvTimer.setVisibility(View.VISIBLE);
        localTimer = new CountDownTimer(seconds * 1000L, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secLeft = (int) (millisUntilFinished / 1000);
                tvTimer.setText(String.format("%02d:%02d", secLeft / 60, secLeft % 60));
            }

            @Override
            public void onFinish() {
                tvTimer.setText("00:00");
            }
        }.start();
    }

    private void updateUI(MafiaState state) {
        if (state == null) return;
        boolean isFirstUpdate = !firstStateReceived;
        firstStateReceived = true;
        lastState = state;

        tvPhase.setText(getString(R.string.mafia_phase_label, state.phase().toString()));
        if (state.viewerRole() != null) {
            tvRole.setText(getString(R.string.mafia_your_role_label, state.viewerRole().name()));
        }

        if (previousPhase == MafiaPhase.GAME_OVER && state.phase() != MafiaPhase.GAME_OVER) {
            resetRoundLocalFlags();
            if (waitingForHostDialog != null && waitingForHostDialog.isShowing()) {
                waitingForHostDialog.dismiss();
                waitingForHostDialog = null;
            }
        }
        previousPhase = state.phase();

        findViewById(R.id.mainLayout).setBackgroundResource(
                state.phase() == MafiaPhase.NIGHT
                        ? R.drawable.background_mafia_night
                        : R.drawable.background_mafia_day);

        boolean isVotePhase = state.phase() == MafiaPhase.DAY_VOTE;
        candidatePanel.setVisibility(isVotePhase ? View.VISIBLE : View.GONE);
        btnSkip.setVisibility(isVotePhase ? View.GONE : View.VISIBLE);

        if (isVotePhase && state.lastDay() != null) {
            tvCandidateName.setText(state.lastDay().candidateName());
            tvGuiltyCount.setText(String.valueOf(state.currentYesVotes()));
            tvNotGuiltyCount.setText(String.valueOf(state.currentNoVotes()));
        }

        if (state.timerEnabled()) {
            startLocalTimer(state.remainingSeconds());
        } else {
            if (localTimer != null) localTimer.cancel();
            tvTimer.setVisibility(View.GONE);
        }

        StringBuilder infoText = new StringBuilder();
        if (state.lastNight() != null) {
            String nightResult = state.lastNight().killedPlayer() != null
                    ? getString(R.string.mafia_someone_died, state.lastNight().killedPlayer())
                    : getString(R.string.mafia_no_one_died);
            infoText.append(getString(R.string.mafia_night_result, nightResult));
        }
        if (state.lastDay() != null) {
            if (state.lastDay().candidateId() != null) {
                infoText.append("\n").append(getString(R.string.mafia_nominated, state.lastDay().candidateName()));
            }
            if (state.lastDay().eliminatedPlayer() != null) {
                infoText.append("\n").append(getString(R.string.mafia_eliminated_by_day, state.lastDay().eliminatedPlayer()));
            }
        }
        tvInfo.setText(infoText.toString());

        List<MafiaPlayerAdapter.Entry> entries = new ArrayList<>();
        List<UUID> mafiaIds = state.knownMafiaIds();
        Map<UUID, Integer> nomCounts = state.nominationCounts() != null ? state.nominationCounts() : Map.of();
        UUID myTarget = state.viewerPendingTarget();

        for (MafiaState.PlayerInfo p : state.players().values()) {
            MafiaPlayerAdapter.Entry entry = new MafiaPlayerAdapter.Entry(p);
            entry.isMafiaBrother = mafiaIds != null && mafiaIds.contains(p.playerId());
            entry.isTargetable = p.alive() && !p.playerId().equals(playerId) && state.phase() != MafiaPhase.GAME_OVER;
            entry.isSelected = p.playerId().equals(myTarget);
            entry.voteCount = nomCounts.getOrDefault(p.playerId(), 0);
            entries.add(entry);
        }
        adapter.updateEntries(entries);

        handleDialogs(state, isFirstUpdate);
    }

    private void handleDialogs(MafiaState state, boolean isFirstUpdate) {
        if (isFirstUpdate) {
            if (state.lastNight() != null) {
                lastNightDialogDay = state.dayNumber();
            }
            if (state.lastDay() != null && state.lastDay().eliminatedPlayer() != null) {
                lastDayEliminationDialogDay = state.dayNumber();
            }
            if (state.myInvestigation() != null) {
                lastInvestigationDialogDay = state.dayNumber();
            }
            if (state.phase() == MafiaPhase.GAME_OVER) {
                winDialogShown = true;
            }
            return;
        }

        boolean gameJustEnded = state.phase() == MafiaPhase.GAME_OVER && !winDialogShown && state.winnerInfo() != null;

        if (!gameJustEnded) {
            if (state.lastNight() != null && state.lastNight().killedPlayer() != null
                    && state.dayNumber() != lastNightDialogDay) {
                lastNightDialogDay = state.dayNumber();
                showNightDeathDialog(state.lastNight());
            }

            if (state.lastDay() != null && state.lastDay().eliminatedPlayer() != null
                    && state.dayNumber() != lastDayEliminationDialogDay) {
                lastDayEliminationDialogDay = state.dayNumber();
                showDayEliminationDialog(state.lastDay());
            }

            if (state.myInvestigation() != null && state.dayNumber() != lastInvestigationDialogDay) {
                lastInvestigationDialogDay = state.dayNumber();
                showInvestigationDialog(state);
            }
        } else {
            if (state.lastNight() != null) {
                lastNightDialogDay = state.dayNumber();
            }
            if (state.lastDay() != null && state.lastDay().eliminatedPlayer() != null) {
                lastDayEliminationDialogDay = state.dayNumber();
            }
        }

        if (gameJustEnded) {
            winDialogShown = true;
            showWinDialog(state.winnerInfo());
        }
    }

    private void showInvestigationDialog(MafiaState state) {
        MafiaState.InvestigationResult inv = state.myInvestigation();
        if (inv == null) return;
        MafiaState.PlayerInfo target = state.players().get(inv.targetId());
        String name = target != null ? target.playerName() : getString(R.string.mafia_unknown_player);
        String verdict = inv.isMafia()
                ? getString(R.string.mafia_is_mafia)
                : getString(R.string.mafia_not_mafia);
        new AlertDialog.Builder(this)
                .setTitle(R.string.mafia_investigation_results)
                .setIcon(inv.isMafia() ? R.drawable.icon_mafia_mafia : R.drawable.icon_mafia_civilian)
                .setMessage(name + " " + verdict)
                .setPositiveButton(R.string.mafia_ok, null)
                .show();
    }

    private void showNightDeathDialog(MafiaState.NightResult result) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.mafia_night_death_title, result.killedPlayer()))
                .setIcon(MafiaPlayerAdapter.getIconForRole(result.killedRole()))
                .setMessage(result.killedRole() != null
                        ? getString(R.string.mafia_role_label, result.killedRole().name())
                        : null)
                .setPositiveButton(R.string.mafia_ok, null)
                .show();
    }

    private void showDayEliminationDialog(MafiaState.DayResult result) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.mafia_day_elimination_title, result.eliminatedPlayer()))
                .setIcon(MafiaPlayerAdapter.getIconForRole(result.eliminatedRole()))
                .setMessage(result.eliminatedRole() != null
                        ? getString(R.string.mafia_role_label, result.eliminatedRole().name())
                        : null)
                .setPositiveButton(R.string.mafia_ok, null)
                .show();
    }

    private void onPlayerClick(MafiaState.PlayerInfo target) {
        if (lastState == null || target.playerId().equals(playerId)) return;

        if (!target.alive() && target.notesRevealed()) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.mafia_notes_title, target.playerName()))
                    .setMessage(target.privateNotes().isEmpty()
                            ? getString(R.string.mafia_no_notes)
                            : target.privateNotes())
                    .setPositiveButton(R.string.mafia_ok, null)
                    .show();
            return;
        }

        MafiaRole myRole = lastState.viewerRole();
        switch (lastState.phase()) {
            case DAY_NOMINATION:
                toggleNomination(target);
                break;
            case NIGHT:
                switch (myRole) {
                    case MAFIA:
                        showConfirm(getString(R.string.mafia_kill_confirm, target.playerName()),
                                () -> sendAction("NIGHT_KILL:" + target.playerId()));
                        break;
                    case DOCTOR:
                        showConfirm(getString(R.string.mafia_save_confirm, target.playerName()),
                                () -> sendAction("NIGHT_SAVE:" + target.playerId()));
                        break;
                    case DETECTIVE:
                        showConfirm(getString(R.string.mafia_investigate_confirm, target.playerName()),
                                () -> sendAction("NIGHT_INVESTIGATE:" + target.playerId()));
                        break;
                    case VIGILANTE:
                        showConfirm(getString(R.string.mafia_shoot_confirm, target.playerName()),
                                () -> sendAction("NIGHT_VIGILANTE_KILL:" + target.playerId()));
                        break;
                    case SERIAL_KILLER:
                        showConfirm(getString(R.string.mafia_kill_confirm, target.playerName()),
                                () -> sendAction("NIGHT_SK_KILL:" + target.playerId()));
                        break;
                    default:
                        Toast.makeText(this, R.string.mafia_no_night_action, Toast.LENGTH_SHORT).show();
                }
                break;
            case DAY_VOTE:
                break;
            default:
        }
    }

    private void toggleNomination(MafiaState.PlayerInfo target) {
        boolean alreadyMyVote = lastState.viewerPendingTarget() != null
                && lastState.viewerPendingTarget().equals(target.playerId());
        if (alreadyMyVote) {
            sendAction("DAY_NOMINATE:SKIP");
        } else {
            sendAction("DAY_NOMINATE:" + target.playerId());
        }
    }

    private void showNotesDialog() {
        if (lastState == null) return;

        EditText input = new EditText(this);
        input.setHint(R.string.mafia_notes_hint);
        input.setText(lastState.viewerPrivateNotes());
        input.setMinLines(4);
        input.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);

        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(pad, pad / 2, pad, pad / 2);
        input.setLayoutParams(params);
        container.addView(input);

        new AlertDialog.Builder(this)
                .setTitle(R.string.mafia_my_notes)
                .setView(container)
                .setPositiveButton(R.string.mafia_save, (d, w) ->
                        sendAction("SET_NOTES:" + input.getText().toString()))
                .setNegativeButton(R.string.mafia_cancel, null)
                .show();
    }

    private void showConfirm(String message, Runnable onConfirm) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.mafia_confirm)
                .setMessage(message)
                .setPositiveButton(R.string.mafia_yes, (d, w) -> onConfirm.run())
                .setNegativeButton(R.string.mafia_no, null)
                .show();
    }

    private void sendAction(String action) {
        if (isHost && game != null) {
            try {
                game.performAction(playerId, action, 0);
                hostServer.broadcastState();
                updateUI(snapshotFor(playerId));
            } catch (Exception e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else if (client != null) {
            client.sendAction(action, 0);
        }
    }

    private void showWinDialog(MafiaState.WinnerInfo info) {
        int iconRes;
        String title;
        switch (info.faction()) {
            case TOWN:
                iconRes = R.drawable.icon_mafia_civilian;
                title = getString(R.string.mafia_town_wins);
                break;
            case MAFIA:
                iconRes = R.drawable.icon_mafia_mafia;
                title = getString(R.string.mafia_mafia_wins);
                break;
            default:
                title = info.neutralRole() == MafiaRole.JESTER
                        ? getString(R.string.mafia_jester_wins)
                        : getString(R.string.mafia_serial_killer_wins);
                iconRes = MafiaPlayerAdapter.getIconForRole(info.neutralRole());
        }
        String message = String.join("\n", info.winnerNames());

        if (isHost) {
            new AlertDialog.Builder(this)
                    .setTitle(title)
                    .setIcon(iconRes)
                    .setMessage(message)
                    .setCancelable(false)
                    .setPositiveButton(R.string.mafia_new_round, (d, w) -> startNewRound())
                    .setNegativeButton(R.string.mafia_end_game, (d, w) -> endSession())
                    .show();
        } else {
            waitingForHostDialog = new AlertDialog.Builder(this)
                    .setTitle(title)
                    .setIcon(iconRes)
                    .setMessage(message + getString(R.string.mafia_waiting_for_host))
                    .setCancelable(false)
                    .show();
        }
    }

    private void startNewRound() {
        try {
            game.startNewRound();
            resetRoundLocalFlags();
            hostServer.broadcastState();
            updateUI(snapshotFor(playerId));
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void endSession() {
        hostServer.broadcastGameOver();
        finish();
    }

    private void resetRoundLocalFlags() {
        winDialogShown = false;
        lastNightDialogDay = -1;
        lastDayEliminationDialogDay = -1;
        lastInvestigationDialogDay = -1;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (localTimer != null) localTimer.cancel();
        if (client != null) client.requestClose();
        ClientHolder.getInstance().clear();
        if (hostServer != null) hostServer.stopServer();
    }
}