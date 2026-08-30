package com.fizzycoyote.pockettable.engine.mafia.role;

import com.fizzycoyote.pockettable.R;
import com.fizzycoyote.pockettable.engine.mafia.MafiaGame;
import com.fizzycoyote.pockettable.engine.mafia.MafiaPlayer;
import com.fizzycoyote.pockettable.engine.mafia.MafiaRole;
import com.fizzycoyote.pockettable.engine.mafia.NightContext;

import java.util.UUID;

public class DoctorBehavior implements RoleBehavior {
    @Override
    public MafiaRole getRole() {
        return MafiaRole.DOCTOR;
    }

    @Override
    public void performNightAction(MafiaGame game, MafiaPlayer actor, String action, UUID target) {
        if (!action.startsWith("NIGHT_SAVE:")) {
            throw new IllegalArgumentException(game.getString(R.string.mafia_error_unknown_doctor_action));
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
        MafiaPlayer doctor = game.findAliveByRole(MafiaRole.DOCTOR);
        if (doctor != null && doctor.getPendingActionTarget() != null) {
            context.doctorSaveTarget = doctor.getPendingActionTarget();
        }
    }
}