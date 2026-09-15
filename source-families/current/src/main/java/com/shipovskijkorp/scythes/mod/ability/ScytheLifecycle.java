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
            PlagueScytheMigrationHandler.migratePlayer(player);
        }
        FireLaunchTracker.tick();
        GoldenLootMarkTracker.tick(server);
    }

    public static void disconnect(ServerPlayer player) {
        BloodHarvestTracker.clear(player);
        ScytheCooldowns.clear(player);
        BloodScytheVampirism.clear(player);
        ToxicAuraTracker.clear(player);

        WitheringAuraTracker.clear(player);

        GoldenLootMarkTracker.clearOwner(player);
        GoldenRainDimensionDayTracker.clear(player);
        ScytheAdvancementTracker.clear(player);
    }
}
