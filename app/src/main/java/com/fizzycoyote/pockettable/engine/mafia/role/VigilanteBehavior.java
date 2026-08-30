package com.fizzycoyote.pockettable.engine.mafia.role;

import com.fizzycoyote.pockettable.R;
import com.fizzycoyote.pockettable.engine.mafia.MafiaGame;
import com.fizzycoyote.pockettable.engine.mafia.MafiaPlayer;
import com.fizzycoyote.pockettable.engine.mafia.MafiaRole;
import com.fizzycoyote.pockettable.engine.mafia.NightContext;

import java.util.UUID;

public class VigilanteBehavior implements RoleBehavior {
    @Override
    public MafiaRole getRole() {
        return MafiaRole.VIGILANTE;
    }

    @Override
    public void performNightAction(MafiaGame game, MafiaPlayer actor, String action, UUID target) {
        if (!action.startsWith("NIGHT_VIGILANTE_KILL:")) {
            throw new IllegalArgumentException(game.getString(R.string.mafia_error_unknown_vigilante_action));
        }
        if (actor.getAbilityCharges() <= 0) {
            throw new IllegalStateException(game.getString(R.string.mafia_error_no_charges));
        }
        game.validateAliveOtherPlayer(target, actor.getPlayerId());
        actor.useAbilityCharge();
        actor.submitAction(target);
    }

    @Override
    public void resolveNight(MafiaGame game, NightContext context) {
        MafiaPlayer vigilante = game.findAliveByRole(MafiaRole.VIGILANTE);
        if (vigilante != null && vigilante.getPendingActionTarget() != null) {
            context.vigilanteTarget = vigilante.getPendingActionTarget();
        }
    }
}