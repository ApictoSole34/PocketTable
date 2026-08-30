package com.fizzycoyote.pockettable.engine.mafia;

public enum MafiaRole {

    MAFIA(Faction.MAFIA),
    DETECTIVE(Faction.TOWN),
    DOCTOR(Faction.TOWN),
    CIVILIAN(Faction.TOWN),

    VIGILANTE(Faction.TOWN),
    MAYOR(Faction.TOWN),
    JESTER(Faction.NEUTRAL),
    SERIAL_KILLER(Faction.NEUTRAL);

    private final Faction faction;

    MafiaRole(Faction faction) {
        this.faction = faction;
    }

    public Faction getFaction() {
        return faction;
    }

    public enum Faction {
        TOWN, MAFIA, NEUTRAL
    }
}
