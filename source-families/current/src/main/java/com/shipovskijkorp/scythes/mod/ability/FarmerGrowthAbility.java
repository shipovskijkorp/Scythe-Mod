package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.item.FarmerScytheItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Active key ability of the Farmer Scythe. */
public final class FarmerGrowthAbility {
    private FarmerGrowthAbility() {}

    public static void tryActivate(ServerPlayer player) {
        InteractionHand hand = FarmerScytheItem.getHeldFarmerScytheHand(player);
        if (hand == null) {
            player.sendOverlayMessage(Component.translatable("message.scythes.farmer_growth.no_scythe"));
            return;
        }

        int cooldownLeft = ScytheCooldowns.remaining(player, ScytheCooldowns.Skill.FARMER_GROWTH);
        if (cooldownLeft > 0) {
            player.sendOverlayMessage(Component.translatable("message.scythes.farmer_growth.cooldown", Math.max(1, (cooldownLeft + 19) / 20)));
            return;
        }

        ItemStack stack = player.getItemInHand(hand);
        if (!FarmerScytheItem.hasEnoughDurability(stack, ScytheBalance.Farmer.GROWTH_DURABILITY_COST)) {
            player.sendOverlayMessage(Component.translatable("message.scythes.scythe_ability.no_durability"));
            return;
        }
        if (!FarmerHarvestHandler.hasBoneMeal(player, ScytheBalance.Farmer.GROWTH_BONE_MEAL_COST)) {
            player.sendOverlayMessage(Component.translatable("message.scythes.farmer_growth.no_bone_meal", ScytheBalance.Farmer.GROWTH_BONE_MEAL_COST));
            return;
        }

        int affected = FarmerHarvestHandler.accelerateNearby(player);
        if (affected <= 0) {
            player.sendOverlayMessage(Component.translatable("message.scythes.farmer_growth.no_crops"));
            return;
        }

        FarmerHarvestHandler.consumeBoneMeal(player, ScytheBalance.Farmer.GROWTH_BONE_MEAL_COST);
        stack.hurtAndBreak(
                ScytheBalance.Farmer.GROWTH_DURABILITY_COST,
                player,
                hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND
        );
        ScytheCooldowns.start(player, ScytheCooldowns.Skill.FARMER_GROWTH, ScytheBalance.Farmer.GROWTH_COOLDOWN_TICKS);
        ScytheAdvancementTracker.markFarmerActive(player);
        player.sendOverlayMessage(Component.translatable("message.scythes.farmer_growth.success", affected));
    }
}
