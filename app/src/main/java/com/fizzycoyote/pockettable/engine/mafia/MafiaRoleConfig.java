package com.fizzycoyote.pockettable.engine.mafia;

import android.content.Context;

import com.fizzycoyote.pockettable.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MafiaRoleConfig {

    private final Context context;

    private int mafia = 1;
    private int detective = 1;
    private int doctor = 1;
    private int vigilante = 0;
    private int mayor = 0;
    private int neutralCount = 0;
    private final List<MafiaRole> allowedNeutralRoles = new ArrayList<>();

    public MafiaRoleConfig(Context context) {
        this.context = (context != null) ? context.getApplicationContext() : null;
    }

    @Deprecated
    public MafiaRoleConfig() {
        this.context = null;
    }

    public int getCount(MafiaRole role) {
        return switch (role) {
            case MAFIA -> mafia;
            case DETECTIVE -> detective;
            case DOCTOR -> doctor;
            case VIGILANTE -> vigilante;
            case MAYOR -> mayor;
            case JESTER, SERIAL_KILLER -> {
                if (neutralCount > 0 && allowedNeutralRoles.contains(role)) {
                    yield 1;
                }
                yield 0;
            }
            default -> 0;
        };
    }

    public void setCount(MafiaRole role, int count) {
        if (count < 0) {
            throw new IllegalArgumentException(getString(R.string.mafia_config_count_negative));
        }
        switch (role) {
            case MAFIA -> mafia = count;
            case DETECTIVE, DOCTOR, VIGILANTE, MAYOR -> {
                if (count > 1) {
                    throw new IllegalArgumentException(
                            String.format(getString(R.string.mafia_config_role_at_most_1), role.name())
                    );
                }
                switch (role) {
                    case DETECTIVE -> detective = count;
                    case DOCTOR -> doctor = count;
                    case VIGILANTE -> vigilante = count;
                    case MAYOR -> mayor = count;
                }
            }
            default -> throw new IllegalArgumentException(
                    getString(R.string.mafia_config_cannot_set_neutral_directly)
            );
        }
    }

    public int getNeutralCount() {
        return neutralCount;
    }

    public void setNeutralCount(int count) {
        if (count < 0) {
            throw new IllegalArgumentException(getString(R.string.mafia_config_neutral_count_negative));
        }
        this.neutralCount = count;
    }

    public void setAllowedNeutralRoles(List<MafiaRole> roles) {
        allowedNeutralRoles.clear();
        for (MafiaRole role : roles) {
            if (role.getFaction() != MafiaRole.Faction.NEUTRAL) {
                throw new IllegalArgumentException(getString(R.string.mafia_config_only_neutral_allowed));
            }
            if (!allowedNeutralRoles.contains(role)) {
                allowedNeutralRoles.add(role);
            }
        }
    }

    public List<MafiaRole> getAllowedNeutralRoles() {
        return new ArrayList<>(allowedNeutralRoles);
    }

    public int getConfiguredSpecialRoleCount() {
        return mafia + detective + doctor + vigilante + mayor + neutralCount;
    }

    public void validateForPlayerCount(int totalPlayers) {
        if (neutralCount > allowedNeutralRoles.size()) {
            throw new IllegalStateException(
                    String.format(
                            getString(R.string.mafia_config_neutral_count_exceeds),
                            neutralCount,
                            allowedNeutralRoles.size()
                    )
            );
        }

        int configured = getConfiguredSpecialRoleCount();
        if (configured > totalPlayers) {
            throw new IllegalStateException(
                    String.format(
                            getString(R.string.mafia_config_roles_exceed_players),
                            configured,
                            totalPlayers
                    )
            );
        }
    }

    public List<MafiaRole> buildRoleList(int totalPlayers) {
        validateForPlayerCount(totalPlayers);

        List<MafiaRole> roles = new ArrayList<>();
        addRoles(roles, MafiaRole.MAFIA, mafia);
        addRoles(roles, MafiaRole.DETECTIVE, detective);
        addRoles(roles, MafiaRole.DOCTOR, doctor);
        addRoles(roles, MafiaRole.VIGILANTE, vigilante);
        addRoles(roles, MafiaRole.MAYOR, mayor);

        List<MafiaRole> shuffledNeutrals = new ArrayList<>(allowedNeutralRoles);
        Collections.shuffle(shuffledNeutrals);
        for (int i = 0; i < neutralCount; i++) {
            roles.add(shuffledNeutrals.get(i));
        }

        while (roles.size() < totalPlayers) {
            roles.add(MafiaRole.CIVILIAN);
        }

        Collections.shuffle(roles);
        return roles;
    }

    private void addRoles(List<MafiaRole> list, MafiaRole role, int count) {
        for (int i = 0; i < count; i++) {
            list.add(role);
        }
    }

    private String getString(int resId) {
        if (context != null) {
            return context.getString(resId);
        } else {
            return "???";
        }
    }

    private String getString(int resId, Object... args) {
        if (context != null) {
            return context.getString(resId, args);
        } else {
            return "???";
        }
    }
}