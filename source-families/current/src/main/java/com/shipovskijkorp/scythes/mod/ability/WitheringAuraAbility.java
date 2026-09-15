package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.item.WitheringScytheItem;
import com.shipovskijkorp.scythes.mod.platform.HudSync;
import com.shipovskijkorp.scythes.mod.platform.HudTransport;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public final class WitheringAuraAbility {

    private WitheringAuraAbility() {
    }

    public static void tryActivate(ServerPlayer player) {
        InteractionHand hand = getHeldWitheringScytheHand(player);
        if (hand == null) {
            player.sendOverlayMessage(Component.translatable("message.scythes.withering_aura.no_scythe"));
            return;
        }

        if (WitheringAuraTracker.isActive(player)) {
            player.sendOverlayMessage(Component.translatable("message.scythes.withering_aura.already_active"));
            return;
        }

        int cooldownLeft = ScytheCooldowns.remaining(player, ScytheCooldowns.Skill.WITHERING_AURA);
        if (cooldownLeft > 0) {
            player.sendOverlayMessage(Component.translatable("message.scythes.withering_aura.cooldown", Math.max(1, cooldownLeft / 20)));
            return;
        }

        ItemStack stack = player.getItemInHand(hand);
        if (!WitheringScytheItem.hasEnoughDurability(stack, ScytheBalance.WitheringAura.AURA_DURABILITY_COST)) {
            player.sendOverlayMessage(Component.translatable("message.scythes.scythe_ability.no_durability"));
            return;
        }

        stack.hurtAndBreak(ScytheBalance.WitheringAura.AURA_DURABILITY_COST, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);

        int auraTicks = WitheringAuraTracker.start(player);
        HudSync.start(player, HudTransport.Timer.WITHERING_AURA, auraTicks);
        ScytheCooldowns.start(player, ScytheCooldowns.Skill.WITHERING_AURA, ScytheBalance.WitheringAura.AURA_COOLDOWN_TICKS);
        ScytheAdvancementTracker.markWitheringActive(player);

        player.level().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.WITHER_AMBIENT,
                SoundSource.PLAYERS,
                0.8F,
                1.15F
        );
        player.sendOverlayMessage(Component.translatable("message.scythes.withering_aura.success"));
    }

    private static InteractionHand getHeldWitheringScytheHand(ServerPlayer player) {
        if (player.getMainHandItem().getItem() instanceof WitheringScytheItem) return InteractionHand.MAIN_HAND;
        if (player.getOffhandItem().getItem() instanceof WitheringScytheItem) return InteractionHand.OFF_HAND;
        return null;
    }
}
