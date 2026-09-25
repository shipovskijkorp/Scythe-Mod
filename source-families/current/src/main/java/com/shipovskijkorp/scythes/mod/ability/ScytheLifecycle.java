package com.shipovskijkorp.scythes.mod.ability;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Loader-neutral server lifecycle. Call from the logical server thread. */
public final class ScytheLifecycle {
    private ScytheLifecycle() {}

    public static void tickServer(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            BloodHarvestTracker.tick(player);
            ToxicAuraTracker.tick(player);
            WitheringAuraTracker.tick(player);
            WitheringMinionManager.tickRestore(player);
            WitheringMinionManager.flushPendingRefunds(player);
            PlagueScytheMigrationHandler.migratePlayer(player);
        }
        DamageAttributionTracker.cleanup(server);
        FireLaunchTracker.tick();
        GoldenLootMarkTracker.tick(server);
    }

    public static void connect(ServerPlayer player) {
        BloodHarvestTracker.connect(player);
        WitheringMinionManager.restoreMinions(player);
        WitheringMinionManager.flushPendingRefunds(player);
    }

    public static void disconnect(ServerPlayer player) {
        BloodHarvestTracker.disconnect(player);
        WitheringMinionManager.suspendMinions(player);
        BloodScytheVampirism.clear(player);
        ToxicAuraTracker.clear(player);
        WitheringAuraTracker.clear(player);
        GoldenLootMarkTracker.clearOwner(player);
        GoldenRainDimensionDayTracker.clear(player);
        ScytheAdvancementTracker.clear(player);
    }
}
