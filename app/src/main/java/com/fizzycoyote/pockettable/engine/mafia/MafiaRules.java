package com.fizzycoyote.pockettable.engine.mafia;

/**
 * Toggleable rules for Mafia. Plain POJO so it serializes cleanly with Gson
 * and can be broadcast as part of {@link com.fizzycoyote.pockettable.models.mafia.MafiaState}.
 *
 * <p>When {@code timerEnabled} is false, a phase only resolves once every
 * player who needs to act has submitted an action ("ręcznie gotowe").
 * When true, the host additionally force-resolves the phase after
 * {@code nightSeconds}/{@code daySeconds}/{@code trialSeconds}, whichever
 * applies to the current phase - though DAY_VOTE (trial) also resolves early
 * as soon as a majority of alive players votes Guilty or Not Guilty.</p>
 */
public class MafiaRules {

    public boolean timerEnabled = false;
    public int nightSeconds = 45;
    public int daySeconds = 90;
    public int trialSeconds = 30;

    public MafiaRules() {
    }

    public MafiaRules(boolean timerEnabled, int nightSeconds, int daySeconds, int trialSeconds) {
        this.timerEnabled = timerEnabled;
        this.nightSeconds = nightSeconds;
        this.daySeconds = daySeconds;
        this.trialSeconds = trialSeconds;
    }

    public boolean timerEnabled() {
        return timerEnabled;
    }

    public int nightSeconds() {
        return nightSeconds;
    }

    public int daySeconds() {
        return daySeconds;
    }

    public int trialSeconds() {
        return trialSeconds;
    }
}