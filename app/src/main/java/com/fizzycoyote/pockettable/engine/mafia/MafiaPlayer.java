package com.fizzycoyote.pockettable.engine.mafia;

import java.util.UUID;

public class MafiaPlayer {

    private final UUID playerId;
    private String playerName;
    private MafiaRole role;
    private boolean alive = true;
    private boolean connected = true;
    private String privateNotes = "";
    private boolean notesRevealed = false;

    private UUID pendingActionTarget = null;
    private boolean hasActed = false;
    private boolean skipped = false;

    private int abilityCharges = 0;

    private boolean mayorRevealed = false;

    public MafiaPlayer(UUID playerId) {
        this.playerId = playerId;
        this.playerName = "Player";
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public MafiaRole getRole() {
        return role;
    }

    public void setRole(MafiaRole role) {
        this.role = role;
    }

    public boolean isAlive() {
        return alive;
    }

    public void kill() {
        this.alive = false;
        this.notesRevealed = true;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public UUID getPendingActionTarget() {
        return pendingActionTarget;
    }

    public boolean hasActed() {
        return hasActed;
    }

    public boolean isSkipped() {
        return skipped;
    }
    public String getPrivateNotes() {
        return privateNotes;
    }

    public void setPrivateNotes(String notes) {
        this.privateNotes = notes;
    }

    public boolean isNotesRevealed() {
        return notesRevealed;
    }

    public void revealNotes() {
        this.notesRevealed = true;
    }

    public void submitAction(UUID targetId) {
        this.pendingActionTarget = targetId;
        this.hasActed = true;
        this.skipped = false;
    }

    public void submitSkip() {
        this.pendingActionTarget = null;
        this.hasActed = true;
        this.skipped = true;
    }

    public void resetForNewPhase() {
        this.pendingActionTarget = null;
        this.hasActed = false;
        this.skipped = false;
    }

    public int getAbilityCharges() {
        return abilityCharges;
    }

    public void setAbilityCharges(int abilityCharges) {
        this.abilityCharges = abilityCharges;
    }

    public boolean useAbilityCharge() {
        if (abilityCharges <= 0) return false;
        abilityCharges--;
        return true;
    }

    public boolean isMayorRevealed() {
        return mayorRevealed;
    }

    public void revealAsMayor() {
        this.mayorRevealed = true;
    }

    public void resetForNewRound() {
        this.alive = true;
        this.mayorRevealed = false;
        this.notesRevealed = false;
        this.privateNotes = "";
        this.abilityCharges = 0;
        resetForNewPhase();
    }
}
