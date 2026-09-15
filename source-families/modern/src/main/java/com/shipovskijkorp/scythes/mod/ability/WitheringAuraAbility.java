package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.item.WitheringScytheItem;
import com.shipovskijkorp.scythes.mod.platform.HudSync;
import com.shipovskijkorp.scythes.mod.platform.HudTransport;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

public final class WitheringAuraAbility {

    private WitheringAuraAbility() {
    }

    public static void tryActivate(ServerPlayerEntity player) {
        Hand hand = getHeldWitheringScytheHand(player);
        if (hand == null) {
            player.sendMessage(Text.translatable("message.scythes.withering_aura.no_scythe"), true);
            return;
        }

        if (WitheringAuraTracker.isActive(player)) {
            player.sendMessage(Text.translatable("message.scythes.withering_aura.already_active"), true);
            return;
        }

        int cooldownLeft = ScytheCooldowns.remaining(player, ScytheCooldowns.Skill.WITHERING_AURA);
        if (cooldownLeft > 0) {
            player.sendMessage(Text.translatable("message.scythes.withering_aura.cooldown", Math.max(1, cooldownLeft / 20)), true);
            return;
        }

        ItemStack stack = player.getStackInHand(hand);
        if (!WitheringScytheItem.hasEnoughDurability(stack, ScytheBalance.WitheringAura.AURA_DURABILITY_COST)) {
            player.sendMessage(Text.translatable("message.scythes.scythe_ability.no_durability"), true);
            return;
        }

        stack.damage(ScytheBalance.WitheringAura.AURA_DURABILITY_COST, player, hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);

        int auraTicks = WitheringAuraTracker.start(player);
        HudSync.start(player, HudTransport.Timer.WITHERING_AURA, auraTicks);
        ScytheCooldowns.start(player, ScytheCooldowns.Skill.WITHERING_AURA, ScytheBalance.WitheringAura.AURA_COOLDOWN_TICKS);
        ScytheAdvancementTracker.markWitheringActive(player);

//? if >=1.21.11 {
        player.getEntityWorld().playSound(
//? } else {
        player.getWorld().playSound(
//? }
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ENTITY_WITHER_AMBIENT,
                SoundCategory.PLAYERS,
                0.8F,
                1.15F
        );
        player.sendMessage(Text.translatable("message.scythes.withering_aura.success"), true);
    }

    private static Hand getHeldWitheringScytheHand(ServerPlayerEntity player) {
        if (player.getMainHandStack().getItem() instanceof WitheringScytheItem) return Hand.MAIN_HAND;
        if (player.getOffHandStack().getItem() instanceof WitheringScytheItem) return Hand.OFF_HAND;
        return null;
    }
}
