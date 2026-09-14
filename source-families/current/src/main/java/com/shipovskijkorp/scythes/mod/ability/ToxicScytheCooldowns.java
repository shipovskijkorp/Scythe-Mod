package com.shipovskijkorp.scythes.mod.ability;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ToxicScytheCooldowns {

    private ToxicScytheCooldowns() {
    }

    private static final Map<UUID, Long> ORB_READY_TICK = new HashMap<>();
    private static final Map<UUID, Long> AURA_READY_TICK = new HashMap<>();

    public static int getOrbTicksLeft(ServerPlayer player) {
        return getTicksLeft(player, ORB_READY_TICK);
    }

    public static int getAuraTicksLeft(ServerPlayer player) {
        return getTicksLeft(player, AURA_READY_TICK);
    }

    public static void setOrbCooldown(ServerPlayer player, int cooldownTicks) {
        setCooldown(player, ORB_READY_TICK, cooldownTicks);
    }

    public static void setAuraCooldown(ServerPlayer player, int cooldownTicks) {
        setCooldown(player, AURA_READY_TICK, cooldownTicks);
    }

    public static void clear(ServerPlayer player) {
        ORB_READY_TICK.remove(player.getUUID());
        AURA_READY_TICK.remove(player.getUUID());
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
