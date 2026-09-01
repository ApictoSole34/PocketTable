package com.fizzycoyote.pockettable.engine.mafia;

import android.content.Context;

import com.fizzycoyote.pockettable.R;
import com.fizzycoyote.pockettable.engine.common.GameEngine;
import com.fizzycoyote.pockettable.engine.mafia.role.CivilianBehavior;
import com.fizzycoyote.pockettable.engine.mafia.role.DetectiveBehavior;
import com.fizzycoyote.pockettable.engine.mafia.role.DoctorBehavior;
import com.fizzycoyote.pockettable.engine.mafia.role.JesterBehavior;
import com.fizzycoyote.pockettable.engine.mafia.role.MafiaBehavior;
import com.fizzycoyote.pockettable.engine.mafia.role.MayorBehavior;
import com.fizzycoyote.pockettable.engine.mafia.role.RoleBehavior;
import com.fizzycoyote.pockettable.engine.mafia.role.SerialKillerBehavior;
import com.fizzycoyote.pockettable.engine.mafia.role.VigilanteBehavior;
import com.fizzycoyote.pockettable.models.mafia.MafiaState;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Core Mafia game engine.
 *
 * <p><b>Thread-safety:</b> instances of this class (and {@link TimedMafiaGame})
 * are mutated from at least three different threads in this app:
 * <ul>
 *     <li>the Android UI thread, when the host acts directly via {@code MafiaTableActivity.sendAction}</li>
 *     <li>the {@link TimedMafiaGame} scheduler thread, which force-resolves phases on timeout</li>
 * </ul>
 * All public methods that read or mutate game state are {@code synchronized}
 * on the {@code MafiaGame} instance itself. {@link TimedMafiaGame} reuses the
 * same intrinsic lock (it is the same object, just a subclass), so its timer
 * callbacks and this class's game logic never interleave in a way that could
 * corrupt state or produce an inconsistent {@link MafiaState} snapshot.</p>
 */
public class MafiaGame implements GameEngine {

    private final Context context;
    private final List<MafiaPlayer> players;
    private MafiaPhase phase;
    private int dayNumber = 0;
    private boolean gameOver = false;
    private String winner = null;
    private MafiaState.WinnerInfo winnerInfo = null;

    private MafiaRules rules;
    private MafiaRoleConfig roleConfig;

    private final Map<MafiaRole, RoleBehavior> roleBehaviors = new EnumMap<>(MafiaRole.class);

    private MafiaState.NightResult lastNightResult;
    private MafiaState.DayResult lastDayResult;
    private final Map<UUID, MafiaState.InvestigationResult> lastInvestigationResults = new HashMap<>();

    private UUID currentCandidateId;

    public MafiaGame(Context context, List<UUID> playerIds) {
        this(context, playerIds, new MafiaRules(), new MafiaRoleConfig());
    }

    public MafiaGame(List<UUID> playerIds) {
        this(null, playerIds, new MafiaRules(), new MafiaRoleConfig());
    }

    public MafiaGame(List<UUID> playerIds, MafiaRules rules, MafiaRoleConfig roleConfig) {
        this(null, playerIds, rules, roleConfig);
    }

    public MafiaGame(Context context, List<UUID> playerIds, MafiaRules rules, MafiaRoleConfig roleConfig) {
        this.context = (context != null) ? context.getApplicationContext() : null;
        this.rules = rules != null ? rules : new MafiaRules();
        this.roleConfig = roleConfig != null ? roleConfig : new MafiaRoleConfig();
        this.players = new ArrayList<>();
        for (UUID id : playerIds) {
            this.players.add(new MafiaPlayer(id));
        }
        registerBehaviors();
    }

    private void registerBehaviors() {
        roleBehaviors.put(MafiaRole.MAFIA, new MafiaBehavior());
        roleBehaviors.put(MafiaRole.DOCTOR, new DoctorBehavior());
        roleBehaviors.put(MafiaRole.DETECTIVE, new DetectiveBehavior());
        roleBehaviors.put(MafiaRole.VIGILANTE, new VigilanteBehavior());
        roleBehaviors.put(MafiaRole.SERIAL_KILLER, new SerialKillerBehavior());
        roleBehaviors.put(MafiaRole.MAYOR, new MayorBehavior());
        roleBehaviors.put(MafiaRole.JESTER, new JesterBehavior());
        roleBehaviors.put(MafiaRole.CIVILIAN, new CivilianBehavior());
    }

    public RoleBehavior getBehavior(MafiaRole role) {
        return roleBehaviors.get(role);
    }

    public String getString(int resId, Object... args) {
        if (context != null) {
            return context.getString(resId, args);
        }
        return "?";
    }

    public synchronized void validateAlivePlayer(UUID targetId) {
        MafiaPlayer target = getPlayer(targetId);
        if (!target.isAlive()) {
            throw new IllegalArgumentException(getString(R.string.mafia_target_not_alive));
        }
    }

    public synchronized void validateAliveOtherPlayer(UUID targetId, UUID actingPlayerId) {
        if (targetId.equals(actingPlayerId)) {
            throw new IllegalArgumentException(getString(R.string.mafia_cannot_target_self));
        }
        validateAlivePlayer(targetId);
    }

    public synchronized UUID tallyMajorityTarget(MafiaRole role) {
        Map<UUID, Integer> counts = new LinkedHashMap<>();
        for (MafiaPlayer p : players) {
            if (p.getRole() == role && p.isAlive() && p.getPendingActionTarget() != null) {
                counts.merge(p.getPendingActionTarget(), 1, Integer::sum);
            }
        }
        return resolveVoteWinner(counts);
    }

    public synchronized MafiaPlayer findAliveByRole(MafiaRole role) {
        for (MafiaPlayer p : players) {
            if (p.isAlive() && p.getRole() == role) return p;
        }
        return null;
    }

    private int aliveCount() {
        return (int) players.stream().filter(MafiaPlayer::isAlive).count();
    }

    public synchronized Map<UUID, Integer> getCurrentNominationCounts() {
        Map<UUID, Integer> counts = new LinkedHashMap<>();
        for (MafiaPlayer p : players) {
            if (p.isAlive() && p.hasActed() && !p.isSkipped() && p.getPendingActionTarget() != null) {
                RoleBehavior behavior = roleBehaviors.get(p.getRole());
                int weight = behavior != null ? behavior.getVoteWeight(p) : 1;
                counts.merge(p.getPendingActionTarget(), weight, Integer::sum);
            }
        }
        return counts;
    }

    public synchronized int getCurrentSkipCount() {
        int count = 0;
        for (MafiaPlayer p : players) {
            if (p.isAlive() && p.hasActed() && p.isSkipped()) count++;
        }
        return count;
    }

    public synchronized int getCurrentYesVotes() {
        int count = 0;
        for (MafiaPlayer p : players) {
            if (p.isAlive() && p.hasActed() && !p.isSkipped()) count++;
        }
        return count;
    }

    public synchronized int getCurrentNoVotes() {
        int count = 0;
        for (MafiaPlayer p : players) {
            if (p.isAlive() && p.hasActed() && p.isSkipped()) count++;
        }
        return count;
    }

    private void assignRoles() {
        List<MafiaRole> roleList = roleConfig.buildRoleList(players.size());
        for (int i = 0; i < players.size(); i++) {
            MafiaPlayer player = players.get(i);
            MafiaRole role = roleList.get(i);
            player.setRole(role);
            if (role == MafiaRole.VIGILANTE) {
                player.setAbilityCharges(1);
            }
        }
    }

    private void resetActionsForNewPhase() {
        for (MafiaPlayer p : players) {
            p.resetForNewPhase();
        }
    }

    private MafiaPlayer applyKillUnlessSaved(UUID targetId, UUID saveTarget) {
        if (targetId == null) return null;
        if (targetId.equals(saveTarget)) return null;
        MafiaPlayer victim = getPlayer(targetId);
        if (victim.isAlive()) {
            victim.kill();
            return victim;
        }
        return null;
    }

    private MafiaPlayer firstNonNullPlayer(MafiaPlayer... values) {
        for (MafiaPlayer v : values) {
            if (v != null) return v;
        }
        return null;
    }

    private void resolveNight() {
        NightContext ctx = new NightContext();
        for (RoleBehavior behavior : roleBehaviors.values()) {
            behavior.resolveNight(this, ctx);
        }

        UUID saveTarget = ctx.doctorSaveTarget;
        boolean mafiaKillSaved = ctx.mafiaKillTarget != null && ctx.mafiaKillTarget.equals(saveTarget);

        MafiaPlayer killedByMafia = applyKillUnlessSaved(ctx.mafiaKillTarget, saveTarget);
        MafiaPlayer killedByVigilante = applyKillUnlessSaved(ctx.vigilanteTarget, saveTarget);
        MafiaPlayer killedBySerialKiller = applyKillUnlessSaved(ctx.serialKillerTarget, saveTarget);

        MafiaPlayer killedPlayer = firstNonNullPlayer(killedByMafia, killedByVigilante, killedBySerialKiller);
        String killedPlayerName = killedPlayer != null ? killedPlayer.getPlayerName() : null;
        MafiaRole killedRole = killedPlayer != null ? killedPlayer.getRole() : null;

        for (NightContext.InvestigationEntry inv : ctx.investigations) {
            lastInvestigationResults.put(
                    inv.detectiveId,
                    new MafiaState.InvestigationResult(inv.targetId, inv.isMafia)
            );
        }

        lastNightResult = new MafiaState.NightResult(killedPlayerName, killedRole, mafiaKillSaved);

        dayNumber++;
        phase = MafiaPhase.DAY_NOMINATION;
        resetActionsForNewPhase();
        checkWinCondition();
        if (!gameOver && this instanceof TimedMafiaGame) {
            ((TimedMafiaGame) this).onPhaseChangedStartTimer();
        }
    }

    private void resolveNomination() {
        Map<UUID, Integer> counts = new LinkedHashMap<>();
        Map<UUID, UUID> nominationReveal = new LinkedHashMap<>();

        for (MafiaPlayer p : players) {
            if (p.isAlive() && p.hasActed()) {
                nominationReveal.put(p.getPlayerId(), p.isSkipped() ? null : p.getPendingActionTarget());
                if (!p.isSkipped() && p.getPendingActionTarget() != null) {
                    RoleBehavior behavior = roleBehaviors.get(p.getRole());
                    int weight = behavior != null ? behavior.getVoteWeight(p) : 1;
                    counts.merge(p.getPendingActionTarget(), weight, Integer::sum);
                }
            }
        }

        UUID candidate = resolveVoteWinner(counts);
        finishNomination(candidate, nominationReveal);
    }

    private void resolveNominationAsSkip() {
        Map<UUID, UUID> nominationReveal = new LinkedHashMap<>();
        for (MafiaPlayer p : players) {
            if (p.isAlive() && p.hasActed()) {
                nominationReveal.put(p.getPlayerId(), p.isSkipped() ? null : p.getPendingActionTarget());
            }
        }
        finishNomination(null, nominationReveal);
    }

    private void finishNomination(UUID candidate, Map<UUID, UUID> nominationReveal) {
        currentCandidateId = candidate;
        String candidateName = null;
        MafiaRole candidateRole = null;
        if (candidate != null) {
            MafiaPlayer c = getPlayer(candidate);
            candidateName = c.getPlayerName();
            candidateRole = c.getRole();
        }

        lastDayResult = new MafiaState.DayResult(
                nominationReveal,
                candidate,
                candidateName,
                candidateRole,
                null,
                null
        );

        if (candidate == null) {
            phase = MafiaPhase.NIGHT;
            resetActionsForNewPhase();
            checkWinCondition();
        } else {
            phase = MafiaPhase.DAY_VOTE;
            resetActionsForNewPhase();
        }
        if (!gameOver && this instanceof TimedMafiaGame) {
            ((TimedMafiaGame) this).onPhaseChangedStartTimer();
        }
    }

    private void resolveDayVote() {
        int yesVotes = getCurrentYesVotes();
        int noVotes = getCurrentNoVotes();

        UUID eliminated = null;
        String eliminatedName = null;
        MafiaRole eliminatedRole = null;

        if (yesVotes > noVotes && currentCandidateId != null) {
            MafiaPlayer victim = getPlayer(currentCandidateId);
            victim.kill();
            eliminated = victim.getPlayerId();
            eliminatedName = victim.getPlayerName();
            eliminatedRole = victim.getRole();
        }

        lastDayResult = new MafiaState.DayResult(
                lastDayResult != null ? lastDayResult.voteMap() : Map.of(),
                currentCandidateId,
                lastDayResult != null ? lastDayResult.candidateName() : null,
                lastDayResult != null ? lastDayResult.candidateRole() : null,
                eliminatedName,
                eliminatedRole
        );

        if (eliminatedRole == MafiaRole.JESTER) {
            winner = "JESTER (" + eliminatedName + ")";
            winnerInfo = new MafiaState.WinnerInfo(
                    MafiaRole.Faction.NEUTRAL, MafiaRole.JESTER, List.of(eliminatedName));
            phase = MafiaPhase.GAME_OVER;
            gameOver = true;
            return;
        }

        phase = MafiaPhase.NIGHT;
        resetActionsForNewPhase();
        checkWinCondition();
        if (!gameOver && this instanceof TimedMafiaGame) {
            ((TimedMafiaGame) this).onPhaseChangedStartTimer();
        }
    }

    private UUID resolveVoteWinner(Map<UUID, Integer> counts) {
        if (counts.isEmpty()) return null;
        int max = Collections.max(counts.values());
        List<UUID> top = new ArrayList<>();
        for (var e : counts.entrySet()) {
            if (e.getValue() == max) top.add(e.getKey());
        }
        return top.size() == 1 ? top.get(0) : null;
    }

    private void checkWinCondition() {
        List<MafiaPlayer> alive = players.stream()
                .filter(MafiaPlayer::isAlive)
                .collect(Collectors.toList());

        long aliveMafia = alive.stream().filter(p -> p.getRole() == MafiaRole.MAFIA).count();
        long aliveSerialKiller = alive.stream().filter(p -> p.getRole() == MafiaRole.SERIAL_KILLER).count();
        long aliveOthers = alive.size() - aliveMafia - aliveSerialKiller;

        if (alive.size() == 1 && aliveSerialKiller == 1) {
            MafiaPlayer sk = alive.get(0);
            winner = "SERIAL_KILLER (" + sk.getPlayerName() + ")";
            winnerInfo = new MafiaState.WinnerInfo(
                    MafiaRole.Faction.NEUTRAL, MafiaRole.SERIAL_KILLER, List.of(sk.getPlayerName()));
            phase = MafiaPhase.GAME_OVER;
            gameOver = true;
            return;
        }

        if (aliveMafia == 0 && aliveSerialKiller == 0) {
            winner = "TOWN";
            winnerInfo = new MafiaState.WinnerInfo(
                    MafiaRole.Faction.TOWN, null, collectNames(MafiaRole.Faction.TOWN));
            phase = MafiaPhase.GAME_OVER;
            gameOver = true;
        } else if (aliveMafia > 0 && aliveMafia >= aliveOthers) {
            winner = "MAFIA";
            winnerInfo = new MafiaState.WinnerInfo(
                    MafiaRole.Faction.MAFIA, null, collectNames(MafiaRole.Faction.MAFIA));
            phase = MafiaPhase.GAME_OVER;
            gameOver = true;
        }
    }

    public synchronized void forceResolveCurrentPhase() {
        if (phase == MafiaPhase.NIGHT) {
            resolveNight();
        } else if (phase == MafiaPhase.DAY_NOMINATION) {
            resolveNomination();
        } else if (phase == MafiaPhase.DAY_VOTE) {
            resolveDayVote();
        }
    }

    private void autoResolveIfReady() {
        if (phase == MafiaPhase.NIGHT) {
            boolean ready = players.stream()
                    .filter(MafiaPlayer::isAlive)
                    .filter(p -> {
                        RoleBehavior b = roleBehaviors.get(p.getRole());
                        return b != null && b.requiresAction();
                    })
                    .allMatch(p -> {
                        RoleBehavior b = roleBehaviors.get(p.getRole());
                        return b.isActionCompleted(this, p);
                    });
            if (ready) resolveNight();
        } else if (phase == MafiaPhase.DAY_NOMINATION) {
            boolean ready = players.stream().filter(MafiaPlayer::isAlive).allMatch(MafiaPlayer::hasActed);
            if (ready) resolveNomination();
        } else if (phase == MafiaPhase.DAY_VOTE) {
            int alive = aliveCount();
            boolean allVoted = players.stream().filter(MafiaPlayer::isAlive).allMatch(MafiaPlayer::hasActed);
            boolean majorityYes = getCurrentYesVotes() > alive / 2;
            boolean majorityNo = getCurrentNoVotes() > alive / 2;
            if (allVoted || majorityYes || majorityNo) {
                resolveDayVote();
            }
        }
    }

    // ========== GameEngine ==========

    @Override
    public synchronized void startGame() {
        if (players.size() < 4) {
            throw new IllegalStateException(getString(R.string.mafia_min_players));
        }
        if (roleConfig.getCount(MafiaRole.MAFIA) < 1) {
            throw new IllegalStateException(getString(R.string.mafia_need_mafia_role));
        }

        roleConfig.validateForPlayerCount(players.size());
        assignRoles();
        dayNumber = 1;
        phase = MafiaPhase.DAY_NOMINATION;
        resetActionsForNewPhase();
    }

    public synchronized void startNewRound() {
        if (roleConfig.getCount(MafiaRole.MAFIA) < 1) {
            throw new IllegalStateException(getString(R.string.mafia_need_mafia_role));
        }
        roleConfig.validateForPlayerCount(players.size());
        for (MafiaPlayer p : players) {
            p.resetForNewRound();
        }
        assignRoles();
        dayNumber = 1;
        gameOver = false;
        winner = null;
        winnerInfo = null;
        lastNightResult = null;
        lastDayResult = null;
        lastInvestigationResults.clear();
        currentCandidateId = null;
        phase = MafiaPhase.DAY_NOMINATION;
        resetActionsForNewPhase();
    }

    /**
     * Executes a player's action in the Mafia game.
     *
     * <p>Actions are phase-dependent. The following formats are supported:</p>
     *
     * <h3>Night Phase ({@link MafiaPhase#NIGHT})</h3>
     * <table border="1" style="border-collapse: collapse; width: 100%;">
     *   <tr style="background: #f0f0f0;">
     *     <th style="padding: 8px;">Role</th>
     *     <th style="padding: 8px;">Action Format</th>
     *     <th style="padding: 8px;">Description</th>
     *   </tr>
     *   <tr>
     *     <td style="padding: 8px;">Mafia</td>
     *     <td style="padding: 8px;"><code>NIGHT_KILL:{uuid}</code></td>
     *     <td style="padding: 8px;">Votes to kill the target player.</td>
     *   </tr>
     *   <tr>
     *     <td style="padding: 8px;">Doctor</td>
     *     <td style="padding: 8px;"><code>NIGHT_SAVE:{uuid}</code></td>
     *     <td style="padding: 8px;">Protects the target player from being killed.</td>
     *   </tr>
     *   <tr>
     *     <td style="padding: 8px;">Detective</td>
     *     <td style="padding: 8px;"><code>NIGHT_INVESTIGATE:{uuid}</code></td>
     *     <td style="padding: 8px;">Investigates whether the target player is Mafia.</td>
     *   </tr>
     *   <tr>
     *     <td style="padding: 8px;">Vigilante</td>
     *     <td style="padding: 8px;"><code>NIGHT_VIGILANTE_KILL:{uuid}</code></td>
     *     <td style="padding: 8px;">Shoots the target player (1 charge per game).</td>
     *   </tr>
     *   <tr>
     *     <td style="padding: 8px;">Serial Killer</td>
     *     <td style="padding: 8px;"><code>NIGHT_SK_KILL:{uuid}</code></td>
     *     <td style="padding: 8px;">Kills the target player.</td>
     *   </tr>
     *   <tr>
     *     <td style="padding: 8px;">Any</td>
     *     <td style="padding: 8px;"><code>NIGHT_PASS</code></td>
     *     <td style="padding: 8px;">Skips the night action.</td>
     *   </tr>
     * </table>
     *
     * <h3>Day Nomination Phase ({@link MafiaPhase#DAY_NOMINATION})</h3>
     * <table border="1" style="border-collapse: collapse; width: 100%;">
     *   <tr style="background: #f0f0f0;">
     *     <th style="padding: 8px;">Action</th>
     *     <th style="padding: 8px;">Format</th>
     *     <th style="padding: 8px;">Description</th>
     *   </tr>
     *   <tr>
     *     <td style="padding: 8px;">Nominate</td>
     *     <td style="padding: 8px;"><code>DAY_NOMINATE:{uuid}</code></td>
     *     <td style="padding: 8px;">Nominates the target player for elimination.</td>
     *   </tr>
     *   <tr>
     *     <td style="padding: 8px;">Skip</td>
     *     <td style="padding: 8px;"><code>DAY_NOMINATE:SKIP</code></td>
     *     <td style="padding: 8px;">Votes to skip nomination (no one is nominated).</td>
     *   </tr>
     *   <tr>
     *     <td style="padding: 8px;">Mayor Reveal</td>
     *     <td style="padding: 8px;"><code>DAY_REVEAL:MAYOR</code></td>
     *     <td style="padding: 8px;">Reveals the player as Mayor (doubles their vote weight).</td>
     *   </tr>
     * </table>
     *
     * <h3>Day Vote Phase ({@link MafiaPhase#DAY_VOTE})</h3>
     * <table border="1" style="border-collapse: collapse; width: 100%;">
     *   <tr style="background: #f0f0f0;">
     *     <th style="padding: 8px;">Action</th>
     *     <th style="padding: 8px;">Format</th>
     *     <th style="padding: 8px;">Description</th>
     *   </tr>
     *   <tr>
     *     <td style="padding: 8px;">Guilty</td>
     *     <td style="padding: 8px;"><code>DAY_VOTE:YES</code></td>
     *     <td style="padding: 8px;">Votes to eliminate the nominated candidate.</td>
     *   </tr>
     *   <tr>
     *     <td style="padding: 8px;">Not Guilty</td>
     *     <td style="padding: 8px;"><code>DAY_VOTE:NO</code></td>
     *     <td style="padding: 8px;">Votes to spare the nominated candidate.</td>
     *   </tr>
     * </table>
     *
     * <h3>Utility Actions (any phase)</h3>
     * <table border="1" style="border-collapse: collapse; width: 100%;">
     *   <tr style="background: #f0f0f0;">
     *     <th style="padding: 8px;">Action</th>
     *     <th style="padding: 8px;">Format</th>
     *     <th style="padding: 8px;">Description</th>
     *   </tr>
     *   <tr>
     *     <td style="padding: 8px;">Set Notes</td>
     *     <td style="padding: 8px;"><code>SET_NOTES:{text}</code></td>
     *     <td style="padding: 8px;">Sets or updates the player's private notes.</td>
     *   </tr>
     * </table>
     *
     * <p><b>Thread-safety:</b> This method is {@code synchronized} on the game instance
     * to prevent concurrent modifications from both the UI thread and the timer thread.</p>
     *
     * @param playerId the UUID of the player performing the action
     * @param action   the action string in one of the formats described above
     * @param amount   numeric parameter (unused in Mafia – always 0)
     * @throws IllegalStateException    if the game is not active, the player is dead,
     *                                  or the action is invalid for the current phase
     * @throws IllegalArgumentException if the action format is invalid, the target doesn't exist,
     *                                  or the player tries to target themselves
     */
    @Override
    public synchronized void performAction(UUID playerId, String action, int amount) {
        if (gameOver || phase == null) {
            throw new IllegalStateException(getString(R.string.mafia_game_inactive));
        }

        MafiaPlayer player = getPlayer(playerId);
        if (!player.isAlive()) {
            throw new IllegalStateException(getString(R.string.mafia_dead_cannot_act));
        }

        if (action.startsWith("SET_NOTES:")) {
            String notes = action.substring("SET_NOTES:".length());
            if (notes.length() > 1000) throw new IllegalArgumentException(getString(R.string.mafia_notes_too_long));
            player.setPrivateNotes(notes);
            return;
        }

        if (phase == MafiaPhase.NIGHT && action.equals("NIGHT_PASS")) {
            player.submitAction(null);
            autoResolveIfReady();
            return;
        }

        if (phase == MafiaPhase.NIGHT) {
            RoleBehavior behavior = roleBehaviors.get(player.getRole());
            if (behavior == null) throw new IllegalArgumentException(
                    getString(R.string.mafia_no_behavior, player.getRole())
            );
            UUID target = extractTarget(action);
            behavior.performNightAction(this, player, action, target);
            autoResolveIfReady();
        } else if (phase == MafiaPhase.DAY_NOMINATION) {
            RoleBehavior behavior = roleBehaviors.get(player.getRole());
            if (behavior != null && (action.startsWith("DAY_REVEAL:") || action.equals("DAY_REVEAL:MAYOR"))) {
                behavior.performDayAction(this, player, action, null);
                return;
            }
            if (action.equals("DAY_NOMINATE:SKIP")) {
                player.submitSkip();
            } else if (action.startsWith("DAY_NOMINATE:")) {
                UUID target = UUID.fromString(action.substring("DAY_NOMINATE:".length()));
                validateAlivePlayer(target);
                player.submitAction(target);
            } else {
                throw new IllegalArgumentException(
                        getString(R.string.mafia_unknown_action, action)
                );
            }

            int alive = aliveCount();
            UUID majority = null;
            for (var e : getCurrentNominationCounts().entrySet()) {
                if (e.getValue() > alive / 2) {
                    majority = e.getKey();
                    break;
                }
            }

            if (majority != null) {
                Map<UUID, UUID> nominationReveal = new LinkedHashMap<>();
                for (MafiaPlayer p : players) {
                    if (p.isAlive() && p.hasActed()) {
                        nominationReveal.put(p.getPlayerId(), p.isSkipped() ? null : p.getPendingActionTarget());
                    }
                }
                finishNomination(majority, nominationReveal);
            } else if (getCurrentSkipCount() > alive / 2) {
                resolveNominationAsSkip();
            } else {
                autoResolveIfReady();
            }
        } else if (phase == MafiaPhase.DAY_VOTE) {
            if (action.equals("DAY_VOTE:YES")) {
                player.submitAction(playerId);
            } else if (action.equals("DAY_VOTE:NO")) {
                player.submitSkip();
            } else {
                throw new IllegalArgumentException(
                        getString(R.string.mafia_unknown_action, action)
                );
            }
            autoResolveIfReady();
        } else {
            throw new IllegalStateException(getString(R.string.mafia_game_over));
        }
    }

    private UUID extractTarget(String action) {
        int colon = action.indexOf(':');
        if (colon == -1) return null;
        return UUID.fromString(action.substring(colon + 1));
    }

    @Override
    public synchronized Object getState(UUID viewerId) {
        return MafiaState.fromGame(this, viewerId);
    }

    @Override
    public synchronized boolean isGameOver() {
        return gameOver;
    }

    public synchronized boolean hasStarted() {
        return phase != null;
    }

    public synchronized void handlePlayerLeft(UUID playerId) {
        MafiaPlayer player = getPlayer(playerId);
        player.setConnected(false);
        if (player.isAlive() && hasStarted()) {
            player.kill();
            checkWinCondition();
            if (!gameOver) autoResolveIfReady();
        }
    }

    private List<String> collectNames(MafiaRole.Faction faction) {
        List<String> names = new ArrayList<>();
        for (MafiaPlayer p : players) {
            if (p.getRole() != null && p.getRole().getFaction() == faction) {
                names.add(p.getPlayerName());
            }
        }
        return names;
    }

    public synchronized List<MafiaPlayer> getPlayers() {
        return new ArrayList<>(players);
    }

    public synchronized boolean hasPlayer(UUID playerId) {
        return players.stream().anyMatch(p -> p.getPlayerId().equals(playerId));
    }

    public synchronized MafiaPlayer getPlayer(UUID playerId) {
        for (MafiaPlayer p : players) {
            if (p.getPlayerId().equals(playerId)) return p;
        }
        throw new IllegalArgumentException(
                getString(R.string.mafia_player_not_found, playerId)
        );
    }

    public synchronized void addPlayer(MafiaPlayer player) {
        players.add(player);
    }

    public synchronized MafiaPhase getPhase() {
        return phase;
    }

    public synchronized int getDayNumber() {
        return dayNumber;
    }

    public synchronized String getWinner() {
        return winner;
    }

    public synchronized MafiaState.WinnerInfo getWinnerInfo() {
        return winnerInfo;
    }

    public synchronized MafiaState.NightResult getLastNightResult() {
        return lastNightResult;
    }

    public synchronized MafiaState.DayResult getLastDayResult() {
        return lastDayResult;
    }

    public synchronized MafiaState.InvestigationResult getLastInvestigationFor(UUID viewerId) {
        return lastInvestigationResults.get(viewerId);
    }

    public synchronized MafiaRules getRules() {
        return rules;
    }

    public Context getContext() {
        return context;
    }

    public synchronized void setRules(MafiaRules rules) {
        this.rules = rules != null ? rules : new MafiaRules();
    }

    public synchronized MafiaRoleConfig getRoleConfig() {
        return roleConfig;
    }

    public synchronized void setRoleConfig(MafiaRoleConfig roleConfig) {
        this.roleConfig = roleConfig != null ? roleConfig : new MafiaRoleConfig();
    }
}