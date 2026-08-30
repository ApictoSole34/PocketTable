package com.fizzycoyote.pockettable.engine.mafia;

import android.content.Context;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * See {@link MafiaGame} for the thread-safety contract this class relies on.
 * All state-touching methods here are {@code synchronized} on the same
 * intrinsic lock ({@code this}) used by {@link MafiaGame}'s own synchronized
 * methods, since this is a subclass and shares the identity of the locked
 * object. This means a timer expiry can never run concurrently with a
 * player's action being processed by {@link MafiaGame#performAction}.
 */
public class TimedMafiaGame extends MafiaGame {

    public interface TimerListener {
        void onTick(int remainingSeconds);
        void onPhaseExpired();
    }

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> timerTask;
    private volatile int remainingSeconds = 0;
    private volatile boolean timerRunning = false;
    private TimerListener timerListener;

    public TimedMafiaGame(Context context, List<UUID> playerIds) {
        super(context, playerIds);
    }

    public TimedMafiaGame(Context context, List<UUID> playerIds, MafiaRules rules, MafiaRoleConfig roleConfig) {
        super(context, playerIds, rules, roleConfig);
    }

    public TimedMafiaGame(List<UUID> playerIds) {
        super(null, playerIds);
    }

    public TimedMafiaGame(List<UUID> playerIds, MafiaRules rules, MafiaRoleConfig roleConfig) {
        super(null, playerIds, rules, roleConfig);
    }

    public void setTimerListener(TimerListener listener) {
        this.timerListener = listener;
    }

    public int getRemainingSeconds() {
        return remainingSeconds;
    }

    public synchronized void startPhaseTimer(int seconds) {
        stopPhaseTimer();
        remainingSeconds = seconds;
        timerRunning = true;
        timerTask = scheduler.scheduleAtFixedRate(this::tick, 0, 1, TimeUnit.SECONDS);
    }

    public synchronized void stopPhaseTimer() {
        if (timerTask != null) {
            timerTask.cancel(false);
            timerTask = null;
        }
        timerRunning = false;
        remainingSeconds = 0;
    }

    private void tick() {
        boolean expired;
        int secondsForTickCallback;

        synchronized (this) {
            if (!timerRunning) return;
            remainingSeconds--;
            secondsForTickCallback = remainingSeconds;
            expired = remainingSeconds <= 0;
            if (expired) {
                timerRunning = false;
            }
        }

        if (timerListener != null) {
            timerListener.onTick(secondsForTickCallback);
        }

        if (expired) {
            synchronized (this) {
                stopPhaseTimer();
                forceResolveCurrentPhase();
            }
            if (timerListener != null) {
                timerListener.onPhaseExpired();
            }
        }
    }

    @Override
    public synchronized void startGame() {
        super.startGame();
        if (getRules().timerEnabled()) {
            onPhaseChangedStartTimer();
        }
    }

    @Override
    public synchronized void startNewRound() {
        super.startNewRound();
        if (getRules().timerEnabled()) {
            onPhaseChangedStartTimer();
        }
    }

    public synchronized void onPhaseChangedStartTimer() {
        if (!getRules().timerEnabled() || isGameOver() || getPhase() == null) {
            stopPhaseTimer();
            return;
        }
        int seconds = switch (getPhase()) {
            case NIGHT -> getRules().nightSeconds();
            case DAY_NOMINATION -> getRules().daySeconds();
            case DAY_VOTE -> getRules().trialSeconds();
            default -> 0;
        };
        if (seconds <= 0) {
            stopPhaseTimer();
            return;
        }
        startPhaseTimer(seconds);
    }

    @Override
    public synchronized void forceResolveCurrentPhase() {
        super.forceResolveCurrentPhase();
    }
}