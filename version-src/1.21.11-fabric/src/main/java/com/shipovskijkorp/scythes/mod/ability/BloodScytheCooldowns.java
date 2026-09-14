package com.shipovskijkorp.scythes.mod.ability;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BloodScytheCooldowns {

    private BloodScytheCooldowns() {
    }

    private static final Map<UUID, Long> BLENDER_READY_TICK = new HashMap<>();

    public static int getBlenderTicksLeft(ServerPlayerEntity player) {
        long readyTick = BLENDER_READY_TICK.getOrDefault(player.getUuid(), 0L);
        long now = player.getEntityWorld().getTime();
        return (int) Math.max(0L, readyTick - now);
    }

    public static void setBlenderCooldown(ServerPlayerEntity player, int cooldownTicks) {
        BLENDER_READY_TICK.put(player.getUuid(), player.getEntityWorld().getTime() + Math.max(0, cooldownTicks));
    }

    public static void clear(ServerPlayerEntity player) {
        BLENDER_READY_TICK.remove(player.getUuid());
    }
}
