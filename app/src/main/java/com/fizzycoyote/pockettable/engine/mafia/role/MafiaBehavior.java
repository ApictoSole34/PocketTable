package com.fizzycoyote.pockettable.engine.mafia.role;

import com.fizzycoyote.pockettable.R;
import com.fizzycoyote.pockettable.engine.mafia.MafiaGame;
import com.fizzycoyote.pockettable.engine.mafia.MafiaPlayer;
import com.fizzycoyote.pockettable.engine.mafia.MafiaRole;
import com.fizzycoyote.pockettable.engine.mafia.NightContext;

import java.util.*;

public class MafiaBehavior implements RoleBehavior {
    @Override
    public MafiaRole getRole() {
        return MafiaRole.MAFIA;
    }

    @Override
    public void performNightAction(MafiaGame game, MafiaPlayer actor, String action, UUID target) {
        if (!action.startsWith("NIGHT_KILL:")) {
            throw new IllegalArgumentException(game.getString(R.string.mafia_error_unknown_mafia_action));
        }
        game.validateAliveOtherPlayer(target, actor.getPlayerId());
        actor.submitAction(target);
    }

    @Override
    public boolean requiresAction() {
        return true;
    }

    @Override
    public boolean isActionCompleted(MafiaGame game, MafiaPlayer actor) {
        return actor.hasActed();
    }

    @Override
    public void resolveNight(MafiaGame game, NightContext context) {
        Map<UUID, Integer> counts = new LinkedHashMap<>();
        for (MafiaPlayer p : game.getPlayers()) {
            if (p.getRole() == MafiaRole.MAFIA && p.isAlive() && p.getPendingActionTarget() != null) {
                counts.merge(p.getPendingActionTarget(), 1, Integer::sum);
            }
        }
        if (counts.isEmpty()) {
            context.mafiaKillTarget = null;
            return;
        }
        int max = Collections.max(counts.values());
        List<UUID> top = new ArrayList<>();
        for (var e : counts.entrySet()) {
            if (e.getValue() == max) top.add(e.getKey());
        }
        context.mafiaKillTarget = top.get(new Random().nextInt(top.size()));
    }
}