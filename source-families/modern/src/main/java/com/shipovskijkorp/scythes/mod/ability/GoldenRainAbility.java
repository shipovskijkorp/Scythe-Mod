package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.item.GoldenScytheItem;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

public final class GoldenRainAbility {

    private GoldenRainAbility() {
    }

    public static void tryActivate(ServerPlayerEntity player) {
        Hand hand = GoldenScytheItem.getHeldGoldenScytheHand(player);
        if (hand == null) {
            player.sendMessage(Text.translatable("message.scythes.golden_rain.no_scythe"), true);
            return;
        }

        ItemStack stack = player.getStackInHand(hand);

        int cooldownLeft = ScytheCooldowns.remaining(player, ScytheCooldowns.Skill.RAIN);
        if (cooldownLeft > 0) {
            player.sendMessage(Text.translatable("message.scythes.golden_rain.cooldown", Math.max(1, cooldownLeft / 20)), true);
            return;
        }

        if (!GoldenScytheItem.hasEnoughDurability(stack, ScytheBalance.GoldenRain.DURABILITY_COST)) {
            player.sendMessage(Text.translatable("message.scythes.scythe_ability.no_durability"), true);
            return;
        }

        int marked = GoldenLootMarkTracker.markAround(player, ScytheBalance.GoldenRain.RADIUS);
        if (marked <= 0) {
            player.sendMessage(Text.translatable("message.scythes.golden_rain.no_targets"), true);
            return;
        }

        stack.damage(ScytheBalance.GoldenRain.DURABILITY_COST, player, hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        ScytheCooldowns.start(player, ScytheCooldowns.Skill.RAIN, ScytheBalance.GoldenRain.COOLDOWN_TICKS);
        ScytheAdvancementTracker.markGoldenActive(player);

//? if >=1.21.11 {
        player.getEntityWorld().playSound(
//? } else {
        player.getWorld().playSound(
//? }
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ENTITY_PLAYER_LEVELUP,
                SoundCategory.PLAYERS,
                0.8F,
                1.45F
        );
        player.sendMessage(Text.translatable("message.scythes.golden_rain.success", marked), true);
    }
}
