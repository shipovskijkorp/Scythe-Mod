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
            WitheringMinionManager.tickRestore(player);
            WitheringMinionManager.flushPendingRefunds(player);
            PlagueScytheMigrationHandler.migratePlayer(player);
        }
        DamageAttributionTracker.cleanup(server);
        FireLaunchTracker.tick();
        GoldenLootMarkTracker.tick(server);
    }

    public static void connect(ServerPlayerEntity player) {
        BloodHarvestTracker.connect(player);
        WitheringMinionManager.restoreMinions(player);
        WitheringMinionManager.flushPendingRefunds(player);
    }

    public static void disconnect(ServerPlayerEntity player) {
        BloodHarvestTracker.disconnect(player);
        WitheringMinionManager.suspendMinions(player);
        BloodScytheVampirism.clear(player);
        ToxicAuraTracker.clear(player);
        WitheringAuraTracker.clear(player);
        GoldenLootMarkTracker.clearOwner(player);
        ScytheAdvancementTracker.clear(player);
    }
}
