package com.shipovskijkorp.scythes.mod.ability;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GoldenScytheCooldowns {

    private GoldenScytheCooldowns() {
    }

    private static final Map<UUID, Long> MIDAS_READY_TICK = new HashMap<>();
    private static final Map<UUID, Long> RAIN_READY_TICK = new HashMap<>();

    public static int getMidasTicksLeft(ServerPlayer player) {
        return getTicksLeft(player, MIDAS_READY_TICK);
    }

    public static void setMidasCooldown(ServerPlayer player, int ticks) {
        setCooldown(player, MIDAS_READY_TICK, ticks);
    }

    public static int getRainTicksLeft(ServerPlayer player) {
        return getTicksLeft(player, RAIN_READY_TICK);
    }

    public static void setRainCooldown(ServerPlayer player, int ticks) {
        setCooldown(player, RAIN_READY_TICK, ticks);
    }

    public static void clear(ServerPlayer player) {
        UUID uuid = player.getUUID();
        MIDAS_READY_TICK.remove(uuid);
        RAIN_READY_TICK.remove(uuid);
    }

    private static int getTicksLeft(ServerPlayer player, Map<UUID, Long> readyTicks) {
        long now = player.level().getGameTime();
        long readyAt = readyTicks.getOrDefault(player.getUUID(), 0L);
        return (int) Math.max(0L, readyAt - now);
    }

    private static void setCooldown(ServerPlayer player, Map<UUID, Long> readyTicks, int ticks) {
        if (ticks <= 0) {
            readyTicks.remove(player.getUUID());
        } else {
            readyTicks.put(player.getUUID(), player.level().getGameTime() + ticks);
        }
    }
}
