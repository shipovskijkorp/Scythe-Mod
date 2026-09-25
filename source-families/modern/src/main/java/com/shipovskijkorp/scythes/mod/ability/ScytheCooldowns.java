package com.shipovskijkorp.scythes.mod.ability;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

/** Persistent per-player ability cooldowns. */
public final class ScytheCooldowns {
    public enum Skill { BLOOD_HARVEST, BLENDER, TOXIC_ORB, TOXIC_AURA, WITHERING_AURA, MIDAS, RAIN, ICE_SPIKE, STORM, FARMER_HARVEST, FARMER_GROWTH, FIREBALL, FIRE_BURST }
    private ScytheCooldowns() {}

    public static int remaining(ServerPlayerEntity player, Skill skill) {
//? if >=1.21.11 {
        if (!(player.getEntityWorld() instanceof ServerWorld world)) return 0;
        MinecraftServer server = world.getServer();
        long now = world.getTime();
//? } else {
        ServerWorld world = player.getServerWorld();
        MinecraftServer server = world.getServer();
        long now = world.getTime();
//? }
        return ScythePersistentStore.remainingCooldown(
                ScytheRuntimeState.worldRoot(server), player.getUuid(), skill.name(), now);
    }

    public static void start(ServerPlayerEntity player, Skill skill, int ticks) {
//? if >=1.21.11 {
        if (!(player.getEntityWorld() instanceof ServerWorld world)) return;
        MinecraftServer server = world.getServer();
        long now = world.getTime();
//? } else {
        ServerWorld world = player.getServerWorld();
        MinecraftServer server = world.getServer();
        long now = world.getTime();
//? }
        ScythePersistentStore.startCooldown(
                ScytheRuntimeState.worldRoot(server), player.getUuid(), skill.name(), now, ticks);
    }

    public static void clear(ServerPlayerEntity player) {}
    public static void clearAll() { ScythePersistentStore.unload(); }
}
