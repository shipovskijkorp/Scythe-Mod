package com.shipovskijkorp.scythes.mod.ability;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BloodScytheCooldowns {

    private BloodScytheCooldowns() {
    }

    private static final Map<UUID, Long> BLENDER_READY_TICK = new HashMap<>();

    public static int getBlenderTicksLeft(ServerPlayer player) {
        long readyTick = BLENDER_READY_TICK.getOrDefault(player.getUUID(), 0L);
        long now = player.level().getGameTime();
        return (int) Math.max(0L, readyTick - now);
    }

    public static void setBlenderCooldown(ServerPlayer player, int cooldownTicks) {
        BLENDER_READY_TICK.put(player.getUUID(), player.level().getGameTime() + Math.max(0, cooldownTicks));
    }

    public static void clear(ServerPlayer player) {
        BLENDER_READY_TICK.remove(player.getUUID());
    }
}
