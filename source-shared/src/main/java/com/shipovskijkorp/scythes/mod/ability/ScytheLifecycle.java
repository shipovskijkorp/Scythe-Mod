package com.shipovskijkorp.scythes.mod.ability;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

/** Loader-neutral server lifecycle. Call from the logical server thread. */
public final class ScytheLifecycle {
    private ScytheLifecycle() {}

    public static void tickServer(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            BloodHarvestTracker.tick(player);
            ToxicAuraTracker.tick(player);
            WitheringAuraTracker.tick(player);
            PlagueScytheMigrationHandler.migratePlayer(player);
        }
        GoldenLootMarkTracker.tick(server);
    }

    public static void disconnect(ServerPlayerEntity player) {
        BloodHarvestTracker.clear(player);
        ScytheCooldowns.clear(player);
        BloodScytheVampirism.clear(player);
        ToxicAuraTracker.clear(player);

        WitheringAuraTracker.clear(player);

        GoldenLootMarkTracker.clearOwner(player);
        ScytheAdvancementTracker.clear(player);
    }
}
