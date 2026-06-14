package com.shipovskijkorp.scythes.mod.ability;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BloodScytheVampirism {

    private BloodScytheVampirism() {
    }

    public static final double VAMPIRISM_CHANCE = 0.25D;
    public static final double HEAL_FRACTION = 0.50D;
    public static final int COOLDOWN_TICKS = 20;

    private static final Map<UUID, Long> LAST_HEAL_TICK = new HashMap<>();

    public static void tryHeal(ServerPlayerEntity player, float actualDamage) {
        if (actualDamage <= 0.0f) return;
        if (!player.isAlive()) return;
        if (player.getHealth() >= player.getMaxHealth()) return;

        long now = player.getWorld().getTime();
        long last = LAST_HEAL_TICK.getOrDefault(player.getUuid(), Long.MIN_VALUE / 2L);
        if (COOLDOWN_TICKS > 0 && now - last < COOLDOWN_TICKS) return;

        if (VAMPIRISM_CHANCE <= 0.0D || player.getRandom().nextDouble() >= VAMPIRISM_CHANCE) return;

        int healAmount = (int) Math.ceil(actualDamage * HEAL_FRACTION);
        if (healAmount <= 0) return;

        player.heal(healAmount);
        LAST_HEAL_TICK.put(player.getUuid(), now);
    }

    public static void clear(ServerPlayerEntity player) {
        LAST_HEAL_TICK.remove(player.getUuid());
    }
}
