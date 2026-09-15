package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

public final class BloodScytheVampirism {

    private BloodScytheVampirism() {
    }

    private static final Map<UUID, Long> LAST_HEAL_TICK = new HashMap<>();

    public static void tryHeal(ServerPlayer player, float actualDamage) {
        if (actualDamage <= 0.0f) return;
        if (!player.isAlive()) return;
        if (player.getHealth() >= player.getMaxHealth()) return;

        long now = player.level().getGameTime();
        long last = LAST_HEAL_TICK.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2L);
        if (ScytheBalance.Vampirism.COOLDOWN_TICKS > 0 && now - last < ScytheBalance.Vampirism.COOLDOWN_TICKS) return;

        if (ScytheBalance.Vampirism.VAMPIRISM_CHANCE <= 0.0D || player.getRandom().nextDouble() >= ScytheBalance.Vampirism.VAMPIRISM_CHANCE) return;

        int healAmount = (int) Math.ceil(actualDamage * ScytheBalance.Vampirism.HEAL_FRACTION);
        if (healAmount <= 0) return;

        player.heal(healAmount);
        LAST_HEAL_TICK.put(player.getUUID(), now);
    }

    public static void clear(ServerPlayer player) {
        LAST_HEAL_TICK.remove(player.getUUID());
    }
}
