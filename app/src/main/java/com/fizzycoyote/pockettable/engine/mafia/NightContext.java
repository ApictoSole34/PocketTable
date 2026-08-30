package com.fizzycoyote.pockettable.engine.mafia;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NightContext {
    public UUID mafiaKillTarget;
    public UUID doctorSaveTarget;
    public UUID vigilanteTarget;
    public UUID serialKillerTarget;

    public static class InvestigationEntry {
        public UUID detectiveId;
        public UUID targetId;
        public boolean isMafia;
    }

    public final List<InvestigationEntry> investigations = new ArrayList<>();

    public void addInvestigation(UUID detectiveId, UUID targetId, boolean isMafia) {
        InvestigationEntry entry = new InvestigationEntry();
        entry.detectiveId = detectiveId;
        entry.targetId = targetId;
        entry.isMafia = isMafia;
        investigations.add(entry);
    }
}