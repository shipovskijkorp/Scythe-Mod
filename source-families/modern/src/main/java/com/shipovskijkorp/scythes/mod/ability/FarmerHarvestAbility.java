package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.item.FarmerScytheItem;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

/** Shift+RMB special of the Farmer Scythe. */
public final class FarmerHarvestAbility {
    private FarmerHarvestAbility() {}

    public static boolean tryActivate(ServerPlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (!(stack.getItem() instanceof FarmerScytheItem)) return false;

        int cooldownLeft = ScytheCooldowns.remaining(player, ScytheCooldowns.Skill.FARMER_HARVEST);
        if (cooldownLeft > 0) {
            player.sendMessage(Text.translatable("message.scythes.farmer_harvest.cooldown", Math.max(1, (cooldownLeft + 19) / 20)), true);
            return false;
        }
        if (!FarmerScytheItem.hasEnoughDurability(stack, ScytheBalance.Farmer.MASS_HARVEST_DURABILITY_COST)) {
            player.sendMessage(Text.translatable("message.scythes.scythe_ability.no_durability"), true);
            return false;
        }

        int harvested = FarmerHarvestHandler.massHarvest(player, stack);
        if (harvested <= 0) {
            player.sendMessage(Text.translatable("message.scythes.farmer_harvest.no_crops"), true);
            return false;
        }

        stack.damage(
                ScytheBalance.Farmer.MASS_HARVEST_DURABILITY_COST,
                player,
                hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND
        );
        ScytheCooldowns.start(player, ScytheCooldowns.Skill.FARMER_HARVEST, ScytheBalance.Farmer.MASS_HARVEST_COOLDOWN_TICKS);
        ScytheAdvancementTracker.markFarmerSpecial(player);
        player.sendMessage(Text.translatable("message.scythes.farmer_harvest.success", harvested), true);
        return true;
    }
}
