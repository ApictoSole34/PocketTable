package com.fizzycoyote.pockettable.engine.mafia.role;

import com.fizzycoyote.pockettable.R;
import com.fizzycoyote.pockettable.engine.mafia.MafiaGame;
import com.fizzycoyote.pockettable.engine.mafia.MafiaPlayer;
import com.fizzycoyote.pockettable.engine.mafia.MafiaRole;

import java.util.UUID;

public class MayorBehavior implements RoleBehavior {
    @Override
    public MafiaRole getRole() {
        return MafiaRole.MAYOR;
    }

    @Override
    public void performNightAction(MafiaGame game, MafiaPlayer actor, String action, UUID target) {}

    @Override
    public void performDayAction(MafiaGame game, MafiaPlayer actor, String action, UUID target) {
        if (action.equals("DAY_REVEAL:MAYOR")) {
            actor.revealAsMayor();
        } else {
            throw new IllegalArgumentException(game.getString(R.string.mafia_error_unknown_mayor_day_action));
        }
    }

    @Override
    public int getVoteWeight(MafiaPlayer player) {
        return player.isMayorRevealed() ? 2 : 1;
    }
}