package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.item.*;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

/** A client requests an action, never its target, damage, cost or cooldown. */
public final class ScytheAbilityHandler {
    private ScytheAbilityHandler() {}

    public static void activate(ServerPlayerEntity player) {
        if (!player.isAlive()) return;
        if (!activateHeld(player, player.getMainHandStack())) {
            activateHeld(player, player.getOffHandStack());
        }
    }

    private static boolean activateHeld(ServerPlayerEntity player, ItemStack stack) {
        if (stack.getItem() instanceof BloodScytheItem) {
            BloodHarvestAbility.tryActivate(player);
            return true;
        }
        if (stack.getItem() instanceof ToxicScytheItem) {
            ToxicAuraAbility.tryActivate(player);
            return true;
        }
        if (stack.getItem() instanceof WitheringScytheItem) {
            WitheringAuraAbility.tryActivate(player);
            return true;
        }
        if (stack.getItem() instanceof GoldenScytheItem) {
            GoldenRainAbility.tryActivate(player);
            return true;
        }
        if (stack.getItem() instanceof FrozenScytheItem) {
            FrozenStormAbility.tryActivate(player);
            return true;
        }
        if (stack.getItem() instanceof FarmerScytheItem) {
            FarmerGrowthAbility.tryActivate(player);
            return true;
        }
        if (stack.getItem() instanceof FireScytheItem) {
            FireBurstAbility.tryActivate(player);
            return true;
        }
        return false;
    }
}
