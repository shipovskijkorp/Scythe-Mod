package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.network.ServerPlayerEntity;

public final class BloodScytheVampirism {

    private BloodScytheVampirism() {
    }

    private static final Map<UUID, Long> LAST_HEAL_TICK = new HashMap<>();

    public static void tryHeal(ServerPlayerEntity player, float actualDamage) {
        if (actualDamage <= 0.0f) return;
        if (!player.isAlive()) return;
        if (player.getHealth() >= player.getMaxHealth()) return;

//? if >=1.21.11 {
        long now = player.getEntityWorld().getTime();
//? } else {
        long now = player.getWorld().getTime();
//? }
        long last = LAST_HEAL_TICK.getOrDefault(player.getUuid(), Long.MIN_VALUE / 2L);
        if (ScytheBalance.Vampirism.COOLDOWN_TICKS > 0 && now - last < ScytheBalance.Vampirism.COOLDOWN_TICKS) return;

        if (ScytheBalance.Vampirism.VAMPIRISM_CHANCE <= 0.0D || player.getRandom().nextDouble() >= ScytheBalance.Vampirism.VAMPIRISM_CHANCE) return;

        int healAmount = (int) Math.ceil(actualDamage * ScytheBalance.Vampirism.HEAL_FRACTION);
        if (healAmount <= 0) return;

        player.heal(healAmount);
        LAST_HEAL_TICK.put(player.getUuid(), now);
    }

    public static void clear(ServerPlayerEntity player) {
        LAST_HEAL_TICK.remove(player.getUuid());
    }
}
