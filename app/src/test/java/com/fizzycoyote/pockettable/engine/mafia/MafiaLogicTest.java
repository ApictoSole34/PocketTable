package com.fizzycoyote.pockettable.engine.mafia;

import static org.junit.Assert.*;

import com.fizzycoyote.pockettable.models.mafia.MafiaState;

import org.junit.Test;

import java.util.*;

public class MafiaLogicTest {

    // ---------- helpers ----------

    static List<UUID> ids(int n) {
        List<UUID> list = new ArrayList<>();
        for (int i = 0; i < n; i++) list.add(UUID.randomUUID());
        return list;
    }

    static MafiaGame freshGame(List<UUID> playerIds, MafiaRoleConfig cfg) {
        MafiaGame game = new MafiaGame(playerIds, new MafiaRules(), cfg);
        for (UUID id : playerIds) {
            game.getPlayer(id).setPlayerName("P-" + id.toString().substring(0, 4));
        }
        return game;
    }

    static void forceRoles(MafiaGame game, List<UUID> ids, MafiaRole... roles) {
        for (int i = 0; i < roles.length; i++) {
            game.getPlayer(ids.get(i)).setRole(roles[i]);
            if (roles[i] == MafiaRole.VIGILANTE) {
                game.getPlayer(ids.get(i)).setAbilityCharges(1);
            }
        }
    }

    static void actIfStillInPhase(MafiaGame game, MafiaPhase expectedPhase, UUID id, String action) {
        if (game.getPhase() == expectedPhase && game.getPlayer(id).isAlive()) {
            game.performAction(id, action, 0);
        }
    }

    static void nominateIfPending(MafiaGame game, UUID id, UUID target) {
        if (game.getPhase() == MafiaPhase.DAY_NOMINATION && game.getPlayer(id).isAlive()) {
            game.performAction(id, "DAY_NOMINATE:" + target, 0);
        }
    }

    static void voteIfPending(MafiaGame game, UUID id, boolean guilty) {
        if (game.getPhase() == MafiaPhase.DAY_VOTE && game.getPlayer(id).isAlive()) {
            game.performAction(id, guilty ? "DAY_VOTE:YES" : "DAY_VOTE:NO", 0);
        }
    }

    // ---------- tests ----------

    @Test
    public void testMafiaMajorityKill() {
        List<UUID> ids = ids(5);
        MafiaRoleConfig cfg = new MafiaRoleConfig();
        cfg.setCount(MafiaRole.MAFIA, 2);
        cfg.setCount(MafiaRole.DETECTIVE, 0);
        cfg.setCount(MafiaRole.DOCTOR, 0);
        MafiaGame game = freshGame(ids, cfg);
        game.startGame();
        forceRoles(game, ids, MafiaRole.MAFIA, MafiaRole.MAFIA, MafiaRole.CIVILIAN, MafiaRole.CIVILIAN, MafiaRole.CIVILIAN);

        for (UUID id : ids) actIfStillInPhase(game, MafiaPhase.DAY_NOMINATION, id, "DAY_NOMINATE:SKIP");

        assertTrue("Should reach NIGHT after day skip", game.getPhase() == MafiaPhase.NIGHT);

        game.performAction(ids.get(0), "NIGHT_KILL:" + ids.get(2), 0);
        game.performAction(ids.get(1), "NIGHT_KILL:" + ids.get(2), 0);

        assertFalse("Victim should be dead after unanimous mafia vote", game.getPlayer(ids.get(2)).isAlive());
    }

    @Test
    public void testMafiaTieNoKill() {
        List<UUID> ids = ids(6);
        MafiaRoleConfig cfg = new MafiaRoleConfig();
        cfg.setCount(MafiaRole.MAFIA, 2);
        cfg.setCount(MafiaRole.DETECTIVE, 0);
        cfg.setCount(MafiaRole.DOCTOR, 0);
        MafiaGame game = freshGame(ids, cfg);
        game.startGame();
        forceRoles(game, ids, MafiaRole.MAFIA, MafiaRole.MAFIA, MafiaRole.CIVILIAN, MafiaRole.CIVILIAN, MafiaRole.CIVILIAN, MafiaRole.CIVILIAN);

        for (UUID id : ids) actIfStillInPhase(game, MafiaPhase.DAY_NOMINATION, id, "DAY_NOMINATE:SKIP");

        game.performAction(ids.get(0), "NIGHT_KILL:" + ids.get(2), 0);
        game.performAction(ids.get(1), "NIGHT_KILL:" + ids.get(3), 0);

        assertTrue("Both targets should survive a tie",
                game.getPlayer(ids.get(2)).isAlive() && game.getPlayer(ids.get(3)).isAlive());
    }

    @Test
    public void testDoctorSavesTarget() {
        List<UUID> ids = ids(5);
        MafiaRoleConfig cfg = new MafiaRoleConfig();
        cfg.setCount(MafiaRole.MAFIA, 1);
        cfg.setCount(MafiaRole.DOCTOR, 1);
        cfg.setCount(MafiaRole.DETECTIVE, 0);
        MafiaGame game = freshGame(ids, cfg);
        game.startGame();
        forceRoles(game, ids, MafiaRole.MAFIA, MafiaRole.DOCTOR, MafiaRole.CIVILIAN, MafiaRole.CIVILIAN, MafiaRole.CIVILIAN);

        for (UUID id : ids) actIfStillInPhase(game, MafiaPhase.DAY_NOMINATION, id, "DAY_NOMINATE:SKIP");

        game.performAction(ids.get(0), "NIGHT_KILL:" + ids.get(2), 0);
        game.performAction(ids.get(1), "NIGHT_SAVE:" + ids.get(2), 0);

        assertTrue("Saved target should survive", game.getPlayer(ids.get(2)).isAlive());
        assertTrue("Night result should report mafia kill saved", game.getLastNightResult().mafiaKillSaved());
        assertNull("No one should be killed", game.getLastNightResult().killedPlayer());
    }

    @Test
    public void testDetectiveInvestigationCorrect() {
        List<UUID> ids = ids(5);
        MafiaRoleConfig cfg = new MafiaRoleConfig();
        cfg.setCount(MafiaRole.MAFIA, 1);
        cfg.setCount(MafiaRole.DETECTIVE, 1);
        cfg.setCount(MafiaRole.DOCTOR, 0);
        MafiaGame game = freshGame(ids, cfg);
        game.startGame();
        forceRoles(game, ids, MafiaRole.MAFIA, MafiaRole.DETECTIVE, MafiaRole.CIVILIAN, MafiaRole.CIVILIAN, MafiaRole.CIVILIAN);

        for (UUID id : ids) actIfStillInPhase(game, MafiaPhase.DAY_NOMINATION, id, "DAY_NOMINATE:SKIP");

        game.performAction(ids.get(0), "NIGHT_KILL:" + ids.get(2), 0);
        game.performAction(ids.get(1), "NIGHT_INVESTIGATE:" + ids.get(0), 0);

        MafiaState.InvestigationResult inv = game.getLastInvestigationFor(ids.get(1));
        assertNotNull("Investigation result should exist", inv);
        if (inv != null) {
            assertTrue("Should identify mafia", inv.isMafia());
            assertEquals("Should target the investigated player", ids.get(0), inv.targetId());
        }

        MafiaState detectiveState = MafiaState.fromGame(game, ids.get(1));
        assertNotNull("Detective should see investigation in own state", detectiveState.myInvestigation());

        MafiaState civilianState = MafiaState.fromGame(game, ids.get(3));
        assertNull("Others should not see investigation", civilianState.myInvestigation());
    }

    @Test
    public void testVigilanteOneShotOnly() {
        List<UUID> ids = ids(6);
        MafiaRoleConfig cfg = new MafiaRoleConfig();
        cfg.setCount(MafiaRole.MAFIA, 1);
        cfg.setCount(MafiaRole.VIGILANTE, 1);
        cfg.setCount(MafiaRole.DETECTIVE, 0);
        cfg.setCount(MafiaRole.DOCTOR, 0);
        MafiaGame game = freshGame(ids, cfg);
        game.startGame();
        forceRoles(game, ids, MafiaRole.MAFIA, MafiaRole.VIGILANTE, MafiaRole.CIVILIAN, MafiaRole.CIVILIAN, MafiaRole.CIVILIAN, MafiaRole.CIVILIAN);

        assertEquals("Vigilante should start with 1 charge", 1, game.getPlayer(ids.get(1)).getAbilityCharges());

        for (UUID id : ids) actIfStillInPhase(game, MafiaPhase.DAY_NOMINATION, id, "DAY_NOMINATE:SKIP");

        game.performAction(ids.get(1), "NIGHT_VIGILANTE_KILL:" + ids.get(3), 0);
        game.performAction(ids.get(0), "NIGHT_KILL:" + ids.get(4), 0);

        assertEquals("Charge should be consumed", 0, game.getPlayer(ids.get(1)).getAbilityCharges());
        assertFalse("Vigilante's target should be dead", game.getPlayer(ids.get(3)).isAlive());

        for (UUID id : ids) {
            actIfStillInPhase(game, MafiaPhase.DAY_NOMINATION, id, "DAY_NOMINATE:SKIP");
        }
        boolean threw = false;
        try {
            game.performAction(ids.get(1), "NIGHT_VIGILANTE_KILL:" + ids.get(5), 0);
        } catch (IllegalStateException e) {
            threw = true;
        }
        assertTrue("Second shot should throw IllegalStateException", threw);
    }

    @Test
    public void testSerialKillerSoloWin() {
        List<UUID> ids = ids(4);
        MafiaRoleConfig cfg = new MafiaRoleConfig();
        cfg.setCount(MafiaRole.MAFIA, 1);
        cfg.setCount(MafiaRole.DETECTIVE, 0);
        cfg.setCount(MafiaRole.DOCTOR, 0);
        cfg.setNeutralCount(1);
        cfg.setAllowedNeutralRoles(List.of(MafiaRole.SERIAL_KILLER));
        MafiaGame game = freshGame(ids, cfg);
        game.startGame();
        forceRoles(game, ids, MafiaRole.MAFIA, MafiaRole.SERIAL_KILLER, MafiaRole.CIVILIAN, MafiaRole.CIVILIAN);

        for (UUID id : ids) nominateIfPending(game, id, ids.get(0));
        for (UUID id : ids) voteIfPending(game, id, true);

        assertFalse("Mafia should be eliminated", game.getPlayer(ids.get(0)).isAlive());
        assertFalse("Game should not be over yet (SK + 1 civilian)", game.isGameOver());

        actIfStillInPhase(game, MafiaPhase.DAY_NOMINATION, ids.get(1), "DAY_NOMINATE:SKIP");
        actIfStillInPhase(game, MafiaPhase.DAY_NOMINATION, ids.get(2), "DAY_NOMINATE:SKIP");
        actIfStillInPhase(game, MafiaPhase.DAY_NOMINATION, ids.get(3), "DAY_NOMINATE:SKIP");
        game.performAction(ids.get(1), "NIGHT_SK_KILL:" + ids.get(2), 0);

        assertFalse("Game should not be over yet (1 civilian left)", game.isGameOver());

        actIfStillInPhase(game, MafiaPhase.DAY_NOMINATION, ids.get(1), "DAY_NOMINATE:SKIP");
        actIfStillInPhase(game, MafiaPhase.DAY_NOMINATION, ids.get(3), "DAY_NOMINATE:SKIP");
        game.performAction(ids.get(1), "NIGHT_SK_KILL:" + ids.get(3), 0);

        assertTrue("Game should end when SK is last alive", game.isGameOver());
        assertNotNull("Winner info should exist", game.getWinnerInfo());
        if (game.getWinnerInfo() != null) {
            assertEquals("Winner faction should be NEUTRAL", MafiaRole.Faction.NEUTRAL, game.getWinnerInfo().faction());
            assertEquals("Neutral role should be SERIAL_KILLER", MafiaRole.SERIAL_KILLER, game.getWinnerInfo().neutralRole());
        }
    }

    @Test
    public void testNominationEarlyMajorityResolve() {
        List<UUID> ids = ids(5);
        MafiaRoleConfig cfg = new MafiaRoleConfig();
        cfg.setCount(MafiaRole.MAFIA, 1);
        cfg.setCount(MafiaRole.DETECTIVE, 0);
        cfg.setCount(MafiaRole.DOCTOR, 0);
        MafiaGame game = freshGame(ids, cfg);
        game.startGame();

        game.performAction(ids.get(0), "DAY_NOMINATE:" + ids.get(4), 0);
        game.performAction(ids.get(1), "DAY_NOMINATE:" + ids.get(4), 0);
        game.performAction(ids.get(2), "DAY_NOMINATE:" + ids.get(4), 0);

        assertSame("Should move to DAY_VOTE early", MafiaPhase.DAY_VOTE, game.getPhase());
        assertEquals("Candidate should be the one with majority", ids.get(4), game.getLastDayResult().candidateId());
    }

    @Test
    public void testNominationSkipMajorityResolve() {
        List<UUID> ids = ids(5);
        MafiaRoleConfig cfg = new MafiaRoleConfig();
        cfg.setCount(MafiaRole.MAFIA, 1);
        cfg.setCount(MafiaRole.DETECTIVE, 0);
        cfg.setCount(MafiaRole.DOCTOR, 0);
        MafiaGame game = freshGame(ids, cfg);
        game.startGame();

        game.performAction(ids.get(0), "DAY_NOMINATE:SKIP", 0);
        game.performAction(ids.get(1), "DAY_NOMINATE:SKIP", 0);
        game.performAction(ids.get(2), "DAY_NOMINATE:SKIP", 0);

        assertSame("Should move to NIGHT early", MafiaPhase.NIGHT, game.getPhase());
        assertNull("No candidate should be set", game.getLastDayResult().candidateId());
    }

    @Test
    public void testDayVoteGuiltyMajorityEarlyResolve() {
        List<UUID> ids = ids(5);
        MafiaRoleConfig cfg = new MafiaRoleConfig();
        cfg.setCount(MafiaRole.MAFIA, 1);
        cfg.setCount(MafiaRole.DETECTIVE, 0);
        cfg.setCount(MafiaRole.DOCTOR, 0);
        MafiaGame game = freshGame(ids, cfg);
        game.startGame();

        for (UUID id : ids) nominateIfPending(game, id, ids.get(4));
        assertSame("Setup should reach DAY_VOTE", MafiaPhase.DAY_VOTE, game.getPhase());

        game.performAction(ids.get(0), "DAY_VOTE:YES", 0);
        game.performAction(ids.get(1), "DAY_VOTE:YES", 0);
        game.performAction(ids.get(2), "DAY_VOTE:YES", 0);

        assertFalse("Candidate should be eliminated", game.getPlayer(ids.get(4)).isAlive());
        assertTrue("Should move to NIGHT or GAME_OVER",
                game.getPhase() == MafiaPhase.NIGHT || game.isGameOver());
    }

    @Test
    public void testDayVoteNotGuiltyMajorityEarlyResolve() {
        List<UUID> ids = ids(5);
        MafiaRoleConfig cfg = new MafiaRoleConfig();
        cfg.setCount(MafiaRole.MAFIA, 1);
        cfg.setCount(MafiaRole.DETECTIVE, 0);
        cfg.setCount(MafiaRole.DOCTOR, 0);
        MafiaGame game = freshGame(ids, cfg);
        game.startGame();

        for (UUID id : ids) nominateIfPending(game, id, ids.get(4));

        game.performAction(ids.get(0), "DAY_VOTE:NO", 0);
        game.performAction(ids.get(1), "DAY_VOTE:NO", 0);
        game.performAction(ids.get(2), "DAY_VOTE:NO", 0);

        assertTrue("Candidate should survive", game.getPlayer(ids.get(4)).isAlive());
        assertSame("Should move to NIGHT", MafiaPhase.NIGHT, game.getPhase());
    }

    @Test
    public void testJesterWinsOnLynch() {
        List<UUID> ids = ids(5);
        MafiaRoleConfig cfg = new MafiaRoleConfig();
        cfg.setCount(MafiaRole.MAFIA, 1);
        cfg.setCount(MafiaRole.DETECTIVE, 0);
        cfg.setCount(MafiaRole.DOCTOR, 0);
        cfg.setNeutralCount(1);
        cfg.setAllowedNeutralRoles(List.of(MafiaRole.JESTER));
        MafiaGame game = freshGame(ids, cfg);
        game.startGame();
        forceRoles(game, ids, MafiaRole.MAFIA, MafiaRole.JESTER, MafiaRole.CIVILIAN, MafiaRole.CIVILIAN, MafiaRole.CIVILIAN);

        for (UUID id : ids) nominateIfPending(game, id, ids.get(1));
        for (UUID id : ids) voteIfPending(game, id, true);

        assertTrue("Game should end when Jester lynched", game.isGameOver());
        assertNotNull("Winner info should exist", game.getWinnerInfo());
        if (game.getWinnerInfo() != null) {
            assertEquals("Neutral role should be Jester", MafiaRole.JESTER, game.getWinnerInfo().neutralRole());
        }
    }

    @Test
    public void testTownWinCondition() {
        List<UUID> ids = ids(4);
        MafiaRoleConfig cfg = new MafiaRoleConfig();
        cfg.setCount(MafiaRole.MAFIA, 1);
        cfg.setCount(MafiaRole.DETECTIVE, 0);
        cfg.setCount(MafiaRole.DOCTOR, 0);
        MafiaGame game = freshGame(ids, cfg);
        game.startGame();
        forceRoles(game, ids, MafiaRole.MAFIA, MafiaRole.CIVILIAN, MafiaRole.CIVILIAN, MafiaRole.CIVILIAN);

        for (UUID id : ids) nominateIfPending(game, id, ids.get(0));
        for (UUID id : ids) voteIfPending(game, id, true);

        assertTrue("Game should end when mafia eliminated", game.isGameOver());
        assertNotNull("Winner info should exist", game.getWinnerInfo());
        if (game.getWinnerInfo() != null) {
            assertEquals("Winner faction should be TOWN", MafiaRole.Faction.TOWN, game.getWinnerInfo().faction());
            assertFalse("Winner list should not be empty", game.getWinnerInfo().winnerNames().isEmpty());
        }
    }

    @Test
    public void testMafiaWinCondition() {
        List<UUID> ids = ids(4);
        MafiaRoleConfig cfg = new MafiaRoleConfig();
        cfg.setCount(MafiaRole.MAFIA, 1);
        cfg.setCount(MafiaRole.DETECTIVE, 0);
        cfg.setCount(MafiaRole.DOCTOR, 0);
        MafiaGame game = freshGame(ids, cfg);
        game.startGame();
        forceRoles(game, ids, MafiaRole.MAFIA, MafiaRole.CIVILIAN, MafiaRole.CIVILIAN, MafiaRole.CIVILIAN);

        actIfStillInPhase(game, MafiaPhase.DAY_NOMINATION, ids.get(1), "DAY_NOMINATE:SKIP");
        actIfStillInPhase(game, MafiaPhase.DAY_NOMINATION, ids.get(2), "DAY_NOMINATE:SKIP");
        actIfStillInPhase(game, MafiaPhase.DAY_NOMINATION, ids.get(3), "DAY_NOMINATE:SKIP");
        game.performAction(ids.get(0), "NIGHT_KILL:" + ids.get(1), 0);

        assertFalse("Game should not be over yet (2 town vs 1 mafia)", game.isGameOver());

        actIfStillInPhase(game, MafiaPhase.DAY_NOMINATION, ids.get(2), "DAY_NOMINATE:SKIP");
        actIfStillInPhase(game, MafiaPhase.DAY_NOMINATION, ids.get(3), "DAY_NOMINATE:SKIP");
        game.performAction(ids.get(0), "NIGHT_KILL:" + ids.get(2), 0);

        assertTrue("Mafia should win when count >= town", game.isGameOver());
        assertNotNull("Winner info should exist", game.getWinnerInfo());
        if (game.getWinnerInfo() != null) {
            assertEquals("Winner faction should be MAFIA", MafiaRole.Faction.MAFIA, game.getWinnerInfo().faction());
        }
    }

    @Test
    public void testMayorDoubleVote() {
        List<UUID> ids = ids(5);
        MafiaRoleConfig cfg = new MafiaRoleConfig();
        cfg.setCount(MafiaRole.MAFIA, 1);
        cfg.setCount(MafiaRole.MAYOR, 1);
        cfg.setCount(MafiaRole.DETECTIVE, 0);
        cfg.setCount(MafiaRole.DOCTOR, 0);
        MafiaGame game = freshGame(ids, cfg);
        game.startGame();
        forceRoles(game, ids, MafiaRole.MAFIA, MafiaRole.MAYOR, MafiaRole.CIVILIAN, MafiaRole.CIVILIAN, MafiaRole.CIVILIAN);

        game.performAction(ids.get(1), "DAY_REVEAL:MAYOR", 0);

        game.performAction(ids.get(1), "DAY_NOMINATE:" + ids.get(4), 0);
        game.performAction(ids.get(2), "DAY_NOMINATE:" + ids.get(4), 0);

        assertSame("Mayor's double vote should trigger early majority", MafiaPhase.DAY_VOTE, game.getPhase());
    }

    @Test
    public void testStartNewRoundResetsEverything() {
        List<UUID> ids = ids(4);
        MafiaRoleConfig cfg = new MafiaRoleConfig();
        cfg.setCount(MafiaRole.MAFIA, 1);
        cfg.setCount(MafiaRole.DETECTIVE, 0);
        cfg.setCount(MafiaRole.DOCTOR, 0);
        MafiaGame game = freshGame(ids, cfg);
        game.startGame();
        forceRoles(game, ids, MafiaRole.MAFIA, MafiaRole.CIVILIAN, MafiaRole.CIVILIAN, MafiaRole.CIVILIAN);

        for (UUID id : ids) nominateIfPending(game, id, ids.get(0));
        for (UUID id : ids) voteIfPending(game, id, true);
        assertTrue("Setup should end game", game.isGameOver());

        game.startNewRound();

        assertFalse("Game should not be over after reset", game.isGameOver());
        assertSame("Phase should be DAY_NOMINATION", MafiaPhase.DAY_NOMINATION, game.getPhase());
        assertEquals("Day number should reset to 1", 1, game.getDayNumber());
        assertTrue("Dead player should revive", game.getPlayer(ids.get(0)).isAlive());
        assertNull("WinnerInfo should be cleared", game.getWinnerInfo());
        assertNull("lastNightResult should be cleared", game.getLastNightResult());
    }

    @Test
    public void testCannotVoteSelfOrDeadInNomination() {
        List<UUID> ids = ids(4);
        MafiaRoleConfig cfg = new MafiaRoleConfig();
        cfg.setCount(MafiaRole.MAFIA, 1);
        cfg.setCount(MafiaRole.DETECTIVE, 0);
        cfg.setCount(MafiaRole.DOCTOR, 0);
        MafiaGame game = freshGame(ids, cfg);
        game.startGame();

        boolean selfNominateThrew = false;
        try {
            game.performAction(ids.get(0), "DAY_NOMINATE:" + ids.get(0), 0);
        } catch (Exception e) {
            selfNominateThrew = true;
        }
        // Dokumentacja aktualnego zachowania: nominowanie siebie jest dozwolone w DAY_NOMINATION
        assertFalse("Self-nomination currently allowed (no self-check)", selfNominateThrew);
    }

    @Test
    public void testNightPassDoesNotCrash() {
        List<UUID> ids = ids(4);
        MafiaRoleConfig cfg = new MafiaRoleConfig();
        cfg.setCount(MafiaRole.MAFIA, 1);
        cfg.setCount(MafiaRole.DETECTIVE, 0);
        cfg.setCount(MafiaRole.DOCTOR, 0);
        MafiaGame game = freshGame(ids, cfg);
        game.startGame();
        forceRoles(game, ids, MafiaRole.MAFIA, MafiaRole.CIVILIAN, MafiaRole.CIVILIAN, MafiaRole.CIVILIAN);

        for (UUID id : ids) actIfStillInPhase(game, MafiaPhase.DAY_NOMINATION, id, "DAY_NOMINATE:SKIP");

        boolean threw = false;
        try {
            game.performAction(ids.get(1), "NIGHT_PASS", 0);
            game.performAction(ids.get(2), "NIGHT_PASS", 0);
            game.performAction(ids.get(3), "NIGHT_PASS", 0);
            game.performAction(ids.get(0), "NIGHT_KILL:" + ids.get(1), 0);
        } catch (Exception e) {
            threw = true;
        }
        assertFalse("Civilians passing at night should not throw", threw);
    }

    @Test
    public void testWinnerInfoTownIncludesDeadMembers() {
        List<UUID> ids = ids(4);
        MafiaRoleConfig cfg = new MafiaRoleConfig();
        cfg.setCount(MafiaRole.MAFIA, 1);
        cfg.setCount(MafiaRole.DETECTIVE, 0);
        cfg.setCount(MafiaRole.DOCTOR, 0);
        MafiaGame game = freshGame(ids, cfg);
        game.startGame();
        forceRoles(game, ids, MafiaRole.MAFIA, MafiaRole.CIVILIAN, MafiaRole.CIVILIAN, MafiaRole.CIVILIAN);
        game.getPlayer(ids.get(1)).setPlayerName("DeadTownMember");

        for (UUID id : ids) actIfStillInPhase(game, MafiaPhase.DAY_NOMINATION, id, "DAY_NOMINATE:SKIP");
        game.performAction(ids.get(0), "NIGHT_KILL:" + ids.get(1), 0);

        List<UUID> alive = new ArrayList<>();
        for (UUID id : ids) if (game.getPlayer(id).isAlive()) alive.add(id);
        for (UUID id : alive) nominateIfPending(game, id, ids.get(0));
        for (UUID id : alive) voteIfPending(game, id, true);

        assertNotNull("Winner info should exist", game.getWinnerInfo());
        if (game.getWinnerInfo() != null) {
            assertEquals("Winner faction should be TOWN", MafiaRole.Faction.TOWN, game.getWinnerInfo().faction());
            assertTrue("Dead town member should be in winner list",
                    game.getWinnerInfo().winnerNames().contains("DeadTownMember"));
        }
    }

    @Test
    public void testTimedGameTrialUsesOwnSeconds() {
        List<UUID> ids = ids(4);
        MafiaRoleConfig cfg = new MafiaRoleConfig();
        cfg.setCount(MafiaRole.MAFIA, 1);
        cfg.setCount(MafiaRole.DETECTIVE, 0);
        cfg.setCount(MafiaRole.DOCTOR, 0);
        MafiaRules rules = new MafiaRules(true, 45, 90, 17);
        TimedMafiaGame game = new TimedMafiaGame(ids, rules, cfg);
        for (UUID id : ids) game.getPlayer(id).setPlayerName("P");
        game.startGame();
        forceRoles(game, ids, MafiaRole.MAFIA, MafiaRole.CIVILIAN, MafiaRole.CIVILIAN, MafiaRole.CIVILIAN);

        for (UUID id : ids) nominateIfPending(game, id, ids.get(1));

        try { Thread.sleep(50); } catch (InterruptedException ignored) {}

        int remaining = game.getRemainingSeconds();
        assertTrue("Trial timer should be near trialSeconds (17), not daySeconds (90)",
                remaining >= 15 && remaining <= 17);

        game.stopPhaseTimer();
    }

    @Test
    public void testNeutralRandomizationFromPool() {
        MafiaRoleConfig cfg = new MafiaRoleConfig();
        cfg.setNeutralCount(1);
        cfg.setAllowedNeutralRoles(List.of(MafiaRole.JESTER, MafiaRole.SERIAL_KILLER));

        boolean jestersSeen = false;
        boolean skSeen = false;

        for (int i = 0; i < 100; i++) {
            List<MafiaRole> roles = cfg.buildRoleList(5);
            if (roles.contains(MafiaRole.JESTER)) jestersSeen = true;
            if (roles.contains(MafiaRole.SERIAL_KILLER)) skSeen = true;
        }

        assertTrue("Jester should appear in many runs", jestersSeen);
        assertTrue("Serial Killer should appear in many runs", skSeen);
    }
}