package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.item.FarmerScytheItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Shift+RMB special of the Farmer Scythe. */
public final class FarmerHarvestAbility {
    private FarmerHarvestAbility() {}

    public static boolean tryActivate(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof FarmerScytheItem)) return false;

        int cooldownLeft = ScytheCooldowns.remaining(player, ScytheCooldowns.Skill.FARMER_HARVEST);
        if (cooldownLeft > 0) {
            player.sendOverlayMessage(Component.translatable("message.scythes.farmer_harvest.cooldown", Math.max(1, (cooldownLeft + 19) / 20)));
            return false;
        }
        if (!FarmerScytheItem.hasEnoughDurability(stack, ScytheBalance.Farmer.MASS_HARVEST_DURABILITY_COST)) {
            player.sendOverlayMessage(Component.translatable("message.scythes.scythe_ability.no_durability"));
            return false;
        }

        int harvested = FarmerHarvestHandler.massHarvest(player, stack);
        if (harvested <= 0) {
            player.sendOverlayMessage(Component.translatable("message.scythes.farmer_harvest.no_crops"));
            return false;
        }

        stack.hurtAndBreak(
                ScytheBalance.Farmer.MASS_HARVEST_DURABILITY_COST,
                player,
                hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND
        );
        ScytheCooldowns.start(player, ScytheCooldowns.Skill.FARMER_HARVEST, ScytheBalance.Farmer.MASS_HARVEST_COOLDOWN_TICKS);
        player.sendOverlayMessage(Component.translatable("message.scythes.farmer_harvest.success", harvested));
        return true;
    }
}
