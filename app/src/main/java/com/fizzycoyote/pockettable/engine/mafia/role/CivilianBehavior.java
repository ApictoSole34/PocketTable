package com.fizzycoyote.pockettable.engine.mafia.role;


import com.fizzycoyote.pockettable.engine.mafia.MafiaGame;
import com.fizzycoyote.pockettable.engine.mafia.MafiaPlayer;
import com.fizzycoyote.pockettable.engine.mafia.MafiaRole;

import java.util.UUID;

public class CivilianBehavior implements RoleBehavior {
    @Override
    public MafiaRole getRole() {
        return MafiaRole.CIVILIAN;
    }

    @Override
    public void performNightAction(MafiaGame game, MafiaPlayer actor, String action, UUID target) {}
}