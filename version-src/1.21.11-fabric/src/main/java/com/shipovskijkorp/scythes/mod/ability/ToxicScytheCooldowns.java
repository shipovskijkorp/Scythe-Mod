package com.shipovskijkorp.scythes.mod.ability;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ToxicScytheCooldowns {

    private ToxicScytheCooldowns() {
    }

    private static final Map<UUID, Long> ORB_READY_TICK = new HashMap<>();
    private static final Map<UUID, Long> AURA_READY_TICK = new HashMap<>();

    public static int getOrbTicksLeft(ServerPlayerEntity player) {
        return getTicksLeft(player, ORB_READY_TICK);
    }

    public static int getAuraTicksLeft(ServerPlayerEntity player) {
        return getTicksLeft(player, AURA_READY_TICK);
    }

    public static void setOrbCooldown(ServerPlayerEntity player, int cooldownTicks) {
        setCooldown(player, ORB_READY_TICK, cooldownTicks);
    }

    public static void setAuraCooldown(ServerPlayerEntity player, int cooldownTicks) {
        setCooldown(player, AURA_READY_TICK, cooldownTicks);
    }

    public static void clear(ServerPlayerEntity player) {
        ORB_READY_TICK.remove(player.getUuid());
        AURA_READY_TICK.remove(player.getUuid());
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
