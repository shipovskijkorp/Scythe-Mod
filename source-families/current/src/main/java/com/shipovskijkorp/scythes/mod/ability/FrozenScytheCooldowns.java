package com.shipovskijkorp.scythes.mod.ability;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-side cooldown storage for Frozen Scythe abilities. */
public final class FrozenScytheCooldowns {

    private static final Map<UUID, Long> ICE_SPIKE_READY_TICK = new HashMap<>();
    private static final Map<UUID, Long> STORM_READY_TICK = new HashMap<>();

    private FrozenScytheCooldowns() {
    }

    public static int getIceSpikeTicksLeft(ServerPlayer player) {
        return getTicksLeft(player, ICE_SPIKE_READY_TICK);
    }

    public static int getStormTicksLeft(ServerPlayer player) {
        return getTicksLeft(player, STORM_READY_TICK);
    }

    public static void setIceSpikeCooldown(ServerPlayer player, int cooldownTicks) {
        setCooldown(player, ICE_SPIKE_READY_TICK, cooldownTicks);
    }

    public static void setStormCooldown(ServerPlayer player, int cooldownTicks) {
        setCooldown(player, STORM_READY_TICK, cooldownTicks);
    }

    public static void clear(ServerPlayer player) {
        ICE_SPIKE_READY_TICK.remove(player.getUUID());
        STORM_READY_TICK.remove(player.getUUID());
    }

    private static int getTicksLeft(ServerPlayer player, Map<UUID, Long> cooldowns) {
        long readyTick = cooldowns.getOrDefault(player.getUUID(), 0L);
        long now = player.level().getGameTime();
        return (int) Math.max(0L, readyTick - now);
    }

    private static void setCooldown(ServerPlayer player, Map<UUID, Long> cooldowns, int cooldownTicks) {
        cooldowns.put(player.getUUID(), player.level().getGameTime() + Math.max(0, cooldownTicks));
    }
}
