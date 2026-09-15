package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.item.GoldenScytheItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class GoldenScytheLootingContext {

    private GoldenScytheLootingContext() {
    }

    private static final ThreadLocal<LivingEntity> CURRENT_LOOT_TARGET = new ThreadLocal<>();

    public static void enter(LivingEntity target) {
        CURRENT_LOOT_TARGET.set(target);
    }

    public static void exit() {
        CURRENT_LOOT_TARGET.remove();
    }

    public static int getLootingBonus(LivingEntity looter) {
        if (!(looter instanceof ServerPlayer player)) return 0;

        int bonus = 0;
        if (GoldenScytheItem.hasGoldenScythe(player)) {
            bonus += ScytheBalance.Golden.PASSIVE_LOOTING_BONUS;
            ScytheAdvancementTracker.markGoldenPassive(player);
        }

        LivingEntity target = CURRENT_LOOT_TARGET.get();
        if (target != null && GoldenLootMarkTracker.isMarkedBy(target, player)) {
            bonus += ScytheBalance.Golden.MARK_LOOTING_BONUS;
        }

        return bonus;
    }
}
