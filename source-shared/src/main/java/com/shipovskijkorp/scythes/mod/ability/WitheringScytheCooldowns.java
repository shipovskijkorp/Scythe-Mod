package com.shipovskijkorp.scythes.mod.ability;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class WitheringScytheCooldowns {

    private WitheringScytheCooldowns() {
    }

    private static final Map<UUID, Long> AURA_READY_TICK = new HashMap<>();

    public static int getAuraTicksLeft(ServerPlayerEntity player) {
        long readyTick = AURA_READY_TICK.getOrDefault(player.getUuid(), 0L);
        long now = player.getWorld().getTime();
        return (int) Math.max(0L, readyTick - now);
    }

    public static void setAuraCooldown(ServerPlayerEntity player, int cooldownTicks) {
        AURA_READY_TICK.put(player.getUuid(), player.getWorld().getTime() + Math.max(0, cooldownTicks));
    }

    public static void clear(ServerPlayerEntity player) {
        AURA_READY_TICK.remove(player.getUuid());
    }
}
