package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.config.ScytheModConfig;
import com.shipovskijkorp.scythes.mod.config.ScytheModConfigLoader;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BloodScytheVampirism {

    private BloodScytheVampirism() {
    }

    private static final Map<UUID, Long> LAST_HEAL_TICK = new HashMap<>();

    public static void tryHeal(ServerPlayerEntity player, float actualDamage) {
        if (actualDamage <= 0.0f) return;
        if (!player.isAlive()) return;
        if (player.getHealth() >= player.getMaxHealth()) return;

        ScytheModConfig config = ScytheModConfigLoader.getConfig();
        int cooldownTicks = Math.max(0, config.bloodVampirismCooldownTicks);
        long now = player.getWorld().getTime();
        long last = LAST_HEAL_TICK.getOrDefault(player.getUuid(), Long.MIN_VALUE / 2L);
        if (cooldownTicks > 0 && now - last < cooldownTicks) return;

        double chance = Math.max(0.0, Math.min(1.0, config.bloodVampirismChance));
        if (chance <= 0.0 || player.getRandom().nextDouble() >= chance) return;

        double healFraction = Math.max(0.0, config.bloodVampirismHealFraction);
        int healAmount = (int) Math.ceil(actualDamage * healFraction);
        if (healAmount <= 0) return;

        player.heal(healAmount);
        LAST_HEAL_TICK.put(player.getUuid(), now);
    }

    public static void clear(ServerPlayerEntity player) {
        LAST_HEAL_TICK.remove(player.getUuid());
    }
}
