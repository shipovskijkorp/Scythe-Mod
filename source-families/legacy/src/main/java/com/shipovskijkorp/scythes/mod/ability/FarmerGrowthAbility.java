package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.item.FarmerScytheItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

/** Active key ability of the Farmer Scythe. */
public final class FarmerGrowthAbility {
    private FarmerGrowthAbility() {}

    public static void tryActivate(ServerPlayerEntity player) {
        Hand hand = FarmerScytheItem.getHeldFarmerScytheHand(player);
        if (hand == null) {
            player.sendMessage(Text.translatable("message.scythes.farmer_growth.no_scythe"), true);
            return;
        }

        int cooldownLeft = ScytheCooldowns.remaining(player, ScytheCooldowns.Skill.FARMER_GROWTH);
        if (cooldownLeft > 0) {
            player.sendMessage(Text.translatable("message.scythes.farmer_growth.cooldown", Math.max(1, (cooldownLeft + 19) / 20)), true);
            return;
        }

        ItemStack stack = player.getStackInHand(hand);
        if (!FarmerScytheItem.hasEnoughDurability(stack, ScytheBalance.Farmer.GROWTH_DURABILITY_COST)) {
            player.sendMessage(Text.translatable("message.scythes.scythe_ability.no_durability"), true);
            return;
        }
        if (!FarmerHarvestHandler.hasBoneMeal(player, ScytheBalance.Farmer.GROWTH_BONE_MEAL_COST)) {
            player.sendMessage(Text.translatable("message.scythes.farmer_growth.no_bone_meal", ScytheBalance.Farmer.GROWTH_BONE_MEAL_COST), true);
            return;
        }

        int affected = FarmerHarvestHandler.accelerateNearby(player);
        if (affected <= 0) {
            player.sendMessage(Text.translatable("message.scythes.farmer_growth.no_crops"), true);
            return;
        }

        FarmerHarvestHandler.consumeBoneMeal(player, ScytheBalance.Farmer.GROWTH_BONE_MEAL_COST);
        stack.damage(ScytheBalance.Farmer.GROWTH_DURABILITY_COST, player, p -> p.sendToolBreakStatus(hand));
        ScytheCooldowns.start(player, ScytheCooldowns.Skill.FARMER_GROWTH, ScytheBalance.Farmer.GROWTH_COOLDOWN_TICKS);
        ScytheAdvancementTracker.markFarmerActive(player);
        player.sendMessage(Text.translatable("message.scythes.farmer_growth.success", affected), true);
    }
}
