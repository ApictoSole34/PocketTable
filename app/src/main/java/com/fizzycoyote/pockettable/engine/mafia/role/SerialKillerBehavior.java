package com.fizzycoyote.pockettable.engine.mafia.role;

import com.fizzycoyote.pockettable.R;
import com.fizzycoyote.pockettable.engine.mafia.MafiaGame;
import com.fizzycoyote.pockettable.engine.mafia.MafiaPlayer;
import com.fizzycoyote.pockettable.engine.mafia.MafiaRole;
import com.fizzycoyote.pockettable.engine.mafia.NightContext;

import java.util.UUID;

public class SerialKillerBehavior implements RoleBehavior {
    @Override
    public MafiaRole getRole() {
        return MafiaRole.SERIAL_KILLER;
    }

    @Override
    public void performNightAction(MafiaGame game, MafiaPlayer actor, String action, UUID target) {
        if (!action.startsWith("NIGHT_SK_KILL:")) {
            throw new IllegalArgumentException(game.getString(R.string.mafia_error_unknown_sk_action));
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
        MafiaPlayer sk = game.findAliveByRole(MafiaRole.SERIAL_KILLER);
        if (sk != null && sk.getPendingActionTarget() != null) {
            context.serialKillerTarget = sk.getPendingActionTarget();
        }
    }
}