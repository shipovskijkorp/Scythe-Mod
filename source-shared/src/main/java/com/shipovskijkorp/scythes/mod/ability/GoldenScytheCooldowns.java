package com.shipovskijkorp.scythes.mod.ability;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GoldenScytheCooldowns {

    private GoldenScytheCooldowns() {
    }

    private static final Map<UUID, Long> MIDAS_READY_TICK = new HashMap<>();
    private static final Map<UUID, Long> RAIN_READY_TICK = new HashMap<>();

    public static int getMidasTicksLeft(ServerPlayerEntity player) {
        return getTicksLeft(player, MIDAS_READY_TICK);
    }

    public static int getRainTicksLeft(ServerPlayerEntity player) {
        return getTicksLeft(player, RAIN_READY_TICK);
    }

    public static void setMidasCooldown(ServerPlayerEntity player, int cooldownTicks) {
        setCooldown(player, MIDAS_READY_TICK, cooldownTicks);
    }

    public static void setRainCooldown(ServerPlayerEntity player, int cooldownTicks) {
        setCooldown(player, RAIN_READY_TICK, cooldownTicks);
    }

    public static void clear(ServerPlayerEntity player) {
        MIDAS_READY_TICK.remove(player.getUuid());
        RAIN_READY_TICK.remove(player.getUuid());
    }

    private static int getTicksLeft(ServerPlayerEntity player, Map<UUID, Long> cooldowns) {
        long readyTick = cooldowns.getOrDefault(player.getUuid(), 0L);
        long now = player.getWorld().getTime();
        return (int) Math.max(0L, readyTick - now);
    }

    private static void setCooldown(ServerPlayerEntity player, Map<UUID, Long> cooldowns, int cooldownTicks) {
        cooldowns.put(player.getUuid(), player.getWorld().getTime() + Math.max(0, cooldownTicks));
    }
}
