package com.fizzycoyote.pockettable.engine.mafia.role;

import com.fizzycoyote.pockettable.R;
import com.fizzycoyote.pockettable.engine.mafia.MafiaGame;
import com.fizzycoyote.pockettable.engine.mafia.MafiaPlayer;
import com.fizzycoyote.pockettable.engine.mafia.MafiaRole;
import com.fizzycoyote.pockettable.engine.mafia.NightContext;

import java.util.UUID;

public class DetectiveBehavior implements RoleBehavior {
    @Override
    public MafiaRole getRole() {
        return MafiaRole.DETECTIVE;
    }

    @Override
    public void performNightAction(MafiaGame game, MafiaPlayer actor, String action, UUID target) {
        if (!action.startsWith("NIGHT_INVESTIGATE:")) {
            throw new IllegalArgumentException(game.getString(R.string.mafia_error_unknown_detective_action));
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
        MafiaPlayer detective = game.findAliveByRole(MafiaRole.DETECTIVE);
        if (detective != null && detective.getPendingActionTarget() != null) {
            UUID targetId = detective.getPendingActionTarget();
            MafiaPlayer target = game.getPlayer(targetId);
            context.addInvestigation(detective.getPlayerId(), targetId, target.getRole() == MafiaRole.MAFIA);
        }
    }
}