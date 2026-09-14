package com.shipovskijkorp.scythes.mod.ability;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-side cooldown storage for Frost Scythe abilities. */
public final class FrozenScytheCooldowns {

    private static final Map<UUID, Long> ICE_SPIKE_READY_TICK = new HashMap<>();
    private static final Map<UUID, Long> STORM_READY_TICK = new HashMap<>();

    private FrozenScytheCooldowns() {
    }

    public static int getIceSpikeTicksLeft(ServerPlayerEntity player) {
        return getTicksLeft(player, ICE_SPIKE_READY_TICK);
    }

    public static int getStormTicksLeft(ServerPlayerEntity player) {
        return getTicksLeft(player, STORM_READY_TICK);
    }

    public static void setIceSpikeCooldown(ServerPlayerEntity player, int cooldownTicks) {
        setCooldown(player, ICE_SPIKE_READY_TICK, cooldownTicks);
    }

    public static void setStormCooldown(ServerPlayerEntity player, int cooldownTicks) {
        setCooldown(player, STORM_READY_TICK, cooldownTicks);
    }

    public static void clear(ServerPlayerEntity player) {
        ICE_SPIKE_READY_TICK.remove(player.getUuid());
        STORM_READY_TICK.remove(player.getUuid());
    }

    private static int getTicksLeft(ServerPlayerEntity player, Map<UUID, Long> cooldowns) {
        long readyTick = cooldowns.getOrDefault(player.getUuid(), 0L);
        long now = player.getEntityWorld().getTime();
        return (int) Math.max(0L, readyTick - now);
    }

    private static void setCooldown(ServerPlayerEntity player, Map<UUID, Long> cooldowns, int cooldownTicks) {
        cooldowns.put(player.getUuid(), player.getEntityWorld().getTime() + Math.max(0, cooldownTicks));
    }
}
