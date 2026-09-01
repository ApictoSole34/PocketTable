package com.fizzycoyote.pockettable.models.mafia;

import com.fizzycoyote.pockettable.engine.mafia.MafiaGame;
import com.fizzycoyote.pockettable.engine.mafia.MafiaPhase;
import com.fizzycoyote.pockettable.engine.mafia.MafiaPlayer;
import com.fizzycoyote.pockettable.engine.mafia.MafiaRole;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Immutable snapshot of the current Mafia game state.
 *
 * <p>This state contains role-specific information – players see different things
 * depending on their role (e.g., Mafia members see other Mafia members).</p>
 *
 * @param phase                 current game phase ({@link MafiaPhase})
 * @param dayNumber             current day number (starts at 1, increments after each night)
 * @param players               map of player IDs to their public information ({@link PlayerInfo})
 * @param viewerId              the UUID of the player who requested this state snapshot
 * @param viewerRole            the role of the viewer, or {@code null} if not known
 * @param knownMafiaIds         list of Mafia member IDs visible to the viewer (only if viewer is Mafia)
 * @param viewerPrivateNotes    the viewer's private notes (only visible to them)
 * @param viewerNotesRevealed   {@code true} if the viewer's notes have been revealed after death
 * @param lastNight             the result of the most recent night phase, or {@code null}
 * @param lastDay               the result of the most recent day phase, or {@code null}
 * @param viewerPendingTarget   the viewer's current action target, or {@code null} if none or already acted
 * @param nominationCounts      current nomination vote counts (non-empty only in {@code DAY_NOMINATION} phase)
 * @param currentYesVotes       number of "Guilty" votes in the current trial (only in {@code DAY_VOTE} phase)
 * @param currentNoVotes        number of "Not Guilty" votes in the current trial (only in {@code DAY_VOTE} phase)
 * @param myInvestigation       the result of the viewer's investigation (only if viewer is Detective and acted)
 * @param winner                raw winner description string (deprecated – prefer {@code winnerInfo})
 * @param winnerInfo            detailed win condition information, or {@code null} if game is not over
 * @param timerEnabled          {@code true} if the game uses a timer for phase resolution
 * @param remainingSeconds      seconds remaining in the current timer, or 0 if timer is disabled
 *
 * @see #fromGame(MafiaGame, UUID)
 */
public record MafiaState(
        MafiaPhase phase,
        int dayNumber,
        Map<UUID, PlayerInfo> players,
        UUID viewerId,
        MafiaRole viewerRole,
        List<UUID> knownMafiaIds,
        String viewerPrivateNotes,
        boolean viewerNotesRevealed,
        NightResult lastNight,
        DayResult lastDay,
        UUID viewerPendingTarget,
        Map<UUID, Integer> nominationCounts,
        int currentYesVotes,
        int currentNoVotes,
        InvestigationResult myInvestigation,
        String winner,
        WinnerInfo winnerInfo,
        boolean timerEnabled,
        int remainingSeconds
) {
    public static MafiaState fromGame(MafiaGame game, UUID viewerId) {
        MafiaPlayer viewer = null;
        if (viewerId != null && game.hasPlayer(viewerId)) {
            viewer = game.getPlayer(viewerId);
        }

        MafiaRole role = viewer != null ? viewer.getRole() : null;

        List<UUID> knownMafia = new ArrayList<>();
        if (role == MafiaRole.MAFIA) {
            for (MafiaPlayer p : game.getPlayers()) {
                if (p.getRole() == MafiaRole.MAFIA) {
                    knownMafia.add(p.getPlayerId());
                }
            }
        }

        boolean viewerIsMafia = (role == MafiaRole.MAFIA);

        Map<UUID, PlayerInfo> infos = game.getPlayers().stream()
                .collect(Collectors.toMap(
                        MafiaPlayer::getPlayerId,
                        p -> PlayerInfo.fromPlayerForViewer(p, viewerId, viewerIsMafia)
                ));

        UUID viewerPendingTarget = (viewer != null && viewer.hasActed() && !viewer.isSkipped())
                ? viewer.getPendingActionTarget()
                : null;

        Map<UUID, Integer> nominationCounts = game.getPhase() == MafiaPhase.DAY_NOMINATION
                ? game.getCurrentNominationCounts()
                : Map.of();

        int yesVotes = game.getPhase() == MafiaPhase.DAY_VOTE ? game.getCurrentYesVotes() : 0;
        int noVotes = game.getPhase() == MafiaPhase.DAY_VOTE ? game.getCurrentNoVotes() : 0;

        return new MafiaState(
                game.getPhase(),
                game.getDayNumber(),
                infos,
                viewerId,
                role,
                knownMafia,
                viewer != null ? viewer.getPrivateNotes() : "",
                viewer != null && viewer.isNotesRevealed(),
                game.getLastNightResult(),
                game.getLastDayResult(),
                viewerPendingTarget,
                nominationCounts,
                yesVotes,
                noVotes,
                game.getLastInvestigationFor(viewerId),
                game.getWinner(),
                game.getWinnerInfo(),
                game.getRules() != null && game.getRules().timerEnabled(),
                game instanceof com.fizzycoyote.pockettable.engine.mafia.TimedMafiaGame
                        ? ((com.fizzycoyote.pockettable.engine.mafia.TimedMafiaGame) game).getRemainingSeconds()
                        : 0
        );
    }

    public record PlayerInfo(
            UUID playerId,
            String playerName,
            boolean alive,
            boolean connected,
            boolean mayorRevealed,
            MafiaRole role,
            String privateNotes,
            boolean notesRevealed,
            UUID votedFor
    ) {
        public static PlayerInfo fromPlayerForViewer(MafiaPlayer p, UUID viewerId, boolean viewerIsMafia) {
            String notes = "";
            if (viewerId != null && viewerId.equals(p.getPlayerId())) {
                notes = p.getPrivateNotes();
            } else if (p.isNotesRevealed()) {
                notes = p.getPrivateNotes();
            }
            boolean isSelf = viewerId != null && viewerId.equals(p.getPlayerId());
            boolean isFellowMafia = viewerIsMafia && p.getRole() == MafiaRole.MAFIA;

            MafiaRole role = null;
            if (isSelf || isFellowMafia) {
                role = p.getRole();
            }

            UUID votedFor = null;
            if (viewerIsMafia && p.getRole() == MafiaRole.MAFIA && p.isAlive() && p.hasActed() && !p.isSkipped()) {
                votedFor = p.getPendingActionTarget();
            }

            return new PlayerInfo(
                    p.getPlayerId(),
                    p.getPlayerName(),
                    p.isAlive(),
                    p.isConnected(),
                    p.isMayorRevealed(),
                    role,
                    notes,
                    p.isNotesRevealed(),
                    votedFor
            );
        }
    }

    public record NightResult(
            String killedPlayer,
            MafiaRole killedRole,
            boolean mafiaKillSaved
    ) {}

    public record DayResult(
            Map<UUID, UUID> voteMap,
            UUID candidateId,
            String candidateName,
            MafiaRole candidateRole,
            String eliminatedPlayer,
            MafiaRole eliminatedRole
    ) {}

    public record WinnerInfo(
            MafiaRole.Faction faction,
            MafiaRole neutralRole,
            List<String> winnerNames
    ) {}

    public record InvestigationResult(
            UUID targetId,
            boolean isMafia
    ) {}
}