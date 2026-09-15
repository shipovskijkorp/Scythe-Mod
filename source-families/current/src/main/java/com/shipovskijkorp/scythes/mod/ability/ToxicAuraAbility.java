package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.item.ToxicScytheItem;
import com.shipovskijkorp.scythes.mod.platform.HudSync;
import com.shipovskijkorp.scythes.mod.platform.HudTransport;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public final class ToxicAuraAbility {

    private ToxicAuraAbility() {
    }

    public static void tryActivate(ServerPlayer player) {
        InteractionHand hand = getHeldToxicScytheHand(player);
        if (hand == null) {
            player.sendOverlayMessage(Component.translatable("message.scythes.toxic_aura.no_scythe"));
            return;
        }

        if (ToxicAuraTracker.isActive(player)) {
            player.sendOverlayMessage(Component.translatable("message.scythes.toxic_aura.already_active"));
            return;
        }

        int cooldownLeft = ScytheCooldowns.remaining(player, ScytheCooldowns.Skill.TOXIC_AURA);
        if (cooldownLeft > 0) {
            player.sendOverlayMessage(Component.translatable("message.scythes.toxic_aura.cooldown", Math.max(1, cooldownLeft / 20)));
            return;
        }

        ItemStack stack = player.getItemInHand(hand);
        if (!ToxicScytheItem.hasEnoughDurability(stack, ScytheBalance.ToxicAura.AURA_DURABILITY_COST)) {
            player.sendOverlayMessage(Component.translatable("message.scythes.scythe_ability.no_durability"));
            return;
        }

        if (ScytheBalance.ToxicAura.AURA_DURABILITY_COST > 0) {
            stack.hurtAndBreak(ScytheBalance.ToxicAura.AURA_DURABILITY_COST, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }

//? if >=26.2 {
        int acidityLevel = ToxicScytheItem.getAcidityLevel(player, stack);
        int auraTicks = ToxicAuraTracker.start(player, acidityLevel);
//? } else {
        int auraTicks = ToxicAuraTracker.start(player, ToxicScytheItem.getAcidityLevel(player, stack));
//? }
        HudSync.start(player, HudTransport.Timer.TOXIC_AURA, auraTicks);
        ScytheCooldowns.start(player, ScytheCooldowns.Skill.TOXIC_AURA, ScytheBalance.ToxicAura.AURA_COOLDOWN_TICKS);
        ScytheAdvancementTracker.markToxicActive(player);

        player.level().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.EVOKER_CAST_SPELL,
                SoundSource.PLAYERS,
                0.8F,
                0.65F
        );
        player.sendOverlayMessage(Component.translatable("message.scythes.toxic_aura.success"));
    }

    private static InteractionHand getHeldToxicScytheHand(ServerPlayer player) {
        if (player.getMainHandItem().getItem() instanceof ToxicScytheItem) return InteractionHand.MAIN_HAND;
        if (player.getOffhandItem().getItem() instanceof ToxicScytheItem) return InteractionHand.OFF_HAND;
        return null;
    }
}
