package com.fizzycoyote.pockettable.engine.mafia.role;

import com.fizzycoyote.pockettable.R;
import com.fizzycoyote.pockettable.engine.mafia.MafiaGame;
import com.fizzycoyote.pockettable.engine.mafia.MafiaPlayer;
import com.fizzycoyote.pockettable.engine.mafia.MafiaRole;
import com.fizzycoyote.pockettable.engine.mafia.NightContext;

import java.util.UUID;

public interface RoleBehavior {
    MafiaRole getRole();

    void performNightAction(MafiaGame game, MafiaPlayer actor, String action, UUID target);

    default boolean requiresAction() {
        return false;
    }

    default boolean isActionCompleted(MafiaGame game, MafiaPlayer actor) {
        return true;
    }

    default void performDayAction(MafiaGame game, MafiaPlayer actor, String action, UUID target) {
        throw new IllegalArgumentException(game.getString(R.string.mafia_error_no_special_day_action));
    }

    default void resolveNight(MafiaGame game, NightContext context) {
    }

    default int getVoteWeight(MafiaPlayer player) {
        return 1;
    }
}