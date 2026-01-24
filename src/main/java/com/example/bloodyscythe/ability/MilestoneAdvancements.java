package com.example.bloodyscythe.ability;

import net.minecraft.advancement.Advancement;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardPlayerScore;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class MilestoneAdvancements {

    private MilestoneAdvancements() {}

    private static final String MOD_ID = "bloodyscythe";
    private static final String CRITERION = "done";

    // objective names MUST be <= 16 chars
    private static final String OBJ_BLOODY_KILLS = "bs_bk";
    private static final String OBJ_PLAGUE_KILLS = "bs_pk";
    private static final String OBJ_WITHER_KILLS = "bs_wk";
    private static final String OBJ_PERF_HARV  = "bs_ph";

    private static final int[] KILL_THRESHOLDS = {1, 5, 10, 20};
    private static final int[] HARV_THRESHOLDS = {1, 5, 10, 15};

    public enum Kind {
        BLOODY("bloody_kills", OBJ_BLOODY_KILLS, KILL_THRESHOLDS),
        PLAGUE("plague_kills", OBJ_PLAGUE_KILLS, KILL_THRESHOLDS),
        WITHERING("withering_kills", OBJ_WITHER_KILLS, KILL_THRESHOLDS),
        PERFECT_HARVEST("perfect_harvest", OBJ_PERF_HARV, HARV_THRESHOLDS);

        public final String advPrefix;
        public final String objective;
        public final int[] thresholds;

        Kind(String advPrefix, String objective, int[] thresholds) {
            this.advPrefix = advPrefix;
            this.objective = objective;
            this.thresholds = thresholds;
        }
    }

    public static void record(ServerPlayerEntity player, Kind kind) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        int value = incrementScore(server, player.getUuidAsString(), kind.objective);

        for (int t : kind.thresholds) {
            if (value >= t) {
                grant(player, kind.advPrefix + "_" + t);
            }
        }
    }

    private static int incrementScore(MinecraftServer server, String entry, String objectiveName) {
        Scoreboard scoreboard = server.getScoreboard();

        ScoreboardObjective obj = scoreboard.getObjective(objectiveName);
        if (obj == null) {
            obj = scoreboard.addObjective(
                    objectiveName,
                    ScoreboardCriterion.DUMMY,
                    Text.literal(objectiveName),
                    ScoreboardCriterion.RenderType.INTEGER
            );
        }

        // ✅ 1.20.1: правильный метод
        ScoreboardPlayerScore score = scoreboard.getPlayerScore(entry, obj);

        int next = score.getScore() + 1;
        score.setScore(next);
        return next;
    }

    private static void grant(ServerPlayerEntity player, String advancementId) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        Advancement adv = server.getAdvancementLoader().get(new Identifier(MOD_ID, advancementId));
        if (adv != null) {
            player.getAdvancementTracker().grantCriterion(adv, CRITERION);
        }
    }
}
