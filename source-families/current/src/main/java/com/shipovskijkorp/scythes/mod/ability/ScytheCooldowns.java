package com.shipovskijkorp.scythes.mod.ability;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Persistent per-player ability cooldowns. */
public final class ScytheCooldowns {
    public enum Skill { BLOOD_HARVEST, BLENDER, TOXIC_ORB, TOXIC_AURA, WITHERING_AURA, MIDAS, RAIN, ICE_SPIKE, STORM, FARMER_HARVEST, FARMER_GROWTH, FIREBALL, FIRE_BURST }
    private ScytheCooldowns() {}

    public static int remaining(ServerPlayer player, Skill skill) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return 0;
        return ScythePersistentStore.remainingCooldown(
                ScytheRuntimeState.worldRoot(server), player.getUUID(), skill.name(), player.level().getGameTime());
    }

    public static void start(ServerPlayer player, Skill skill, int ticks) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        ScythePersistentStore.startCooldown(
                ScytheRuntimeState.worldRoot(server), player.getUUID(), skill.name(), player.level().getGameTime(), ticks);
    }

    public static void clear(ServerPlayer player) {}
    public static void clearAll() { ScythePersistentStore.unload(); }
}
