package com.shipovskijkorp.scythes.mod.ability;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

/** Persistent per-player ability cooldowns. */
public final class ScytheCooldowns {
    public enum Skill { BLOOD_HARVEST, BLENDER, TOXIC_ORB, TOXIC_AURA, WITHERING_AURA, MIDAS, RAIN, ICE_SPIKE, STORM, FARMER_HARVEST, FARMER_GROWTH, FIREBALL, FIRE_BURST }
    private ScytheCooldowns() {}

    public static int remaining(ServerPlayerEntity player, Skill skill) {
        MinecraftServer server = player.getServerWorld().getServer();
        if (server == null) return 0;
        return ScythePersistentStore.remainingCooldown(
                ScytheRuntimeState.worldRoot(server),
                player.getUuid(),
                skill.name(),
                player.getWorld().getTime()
        );
    }

    public static void start(ServerPlayerEntity player, Skill skill, int ticks) {
        MinecraftServer server = player.getServerWorld().getServer();
        if (server == null) return;
        ScythePersistentStore.startCooldown(
                ScytheRuntimeState.worldRoot(server),
                player.getUuid(),
                skill.name(),
                player.getWorld().getTime(),
                ticks
        );
    }

    /** Reconnects must not reset gameplay cooldowns. */
    public static void clear(ServerPlayerEntity player) {}

    /** Drops only the process cache; the world data file remains authoritative. */
    public static void clearAll() { ScythePersistentStore.unload(); }
}
