package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.item.BloodScytheItem;
import com.shipovskijkorp.scythes.mod.network.BloodHarvestHudS2CPacket;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class BloodHarvestAbility {

    public static final double RADIUS = 10.0D;
    public static final int COOLDOWN_TICKS = 20 * 60;
    public static final int DURABILITY_COST = 100;

    public static final int SLOWNESS_TICKS = 20 * 10;
    public static final int BLINDNESS_TICKS = 20 * 10;
    public static final int WEAKNESS_TICKS = 20 * 10;
    public static final int GLOWING_TICKS = 20 * 20;
    public static final int SLOWNESS_AMPLIFIER = 1;
    public static final int WEAKNESS_AMPLIFIER = 1;

    /** Активация способности */
    public static void tryActivate(ServerPlayer player) {

        InteractionHand hand = getHeldBloodScytheHand(player);
        if (hand == null) {
            player.sendOverlayMessage(Component.translatable("message.scythes.blood_harvest.no_scythe"));
            return;
        }

        ItemStack stack = player.getItemInHand(hand);
        ServerLevel world = (ServerLevel) player.level();

        if (DURABILITY_COST > 0) {
            int remaining = stack.getMaxDamage() - stack.getDamageValue();
            if (remaining < DURABILITY_COST) {
                player.sendOverlayMessage(Component.translatable("message.scythes.blood_harvest.no_durability"));
                return;
            }
        }

        if (player.getCooldowns().isOnCooldown(stack)) {
            player.sendOverlayMessage(Component.translatable("message.scythes.blood_harvest.cooldown"));
            return;
        }

        AABB box = player.getBoundingBox().inflate(RADIUS);
        List<ServerPlayer> targets =
                world.getEntitiesOfClass(
                        ServerPlayer.class,
                        box,
                        p -> p != player && p.isAlive() && !p.isSpectator() && !player.isAlliedTo(p)
                );

        if (targets.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.scythes.blood_harvest.no_targets"));
            return;
        }

        // ✅ тратим прочность ТОЛЬКО на активированной косе
        if (DURABILITY_COST > 0) {
            stack.hurtAndBreak(DURABILITY_COST, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }

        player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);

        for (ServerPlayer target : targets) {
            ScytheAdvancementTracker.markBloodActive(player, target);
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, SLOWNESS_TICKS, SLOWNESS_AMPLIFIER));
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLINDNESS_TICKS, 0));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, WEAKNESS_TICKS, WEAKNESS_AMPLIFIER));
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOWING_TICKS, 0));
        }

        int windowTicks = BloodHarvestTracker.start(player);
        BloodHarvestHudS2CPacket.sendTicks(player, windowTicks);

        player.sendOverlayMessage(Component.translatable("message.scythes.blood_harvest.success"));
    }

    private static InteractionHand getHeldBloodScytheHand(ServerPlayer player) {
        if (player.getMainHandItem().getItem() instanceof BloodScytheItem) return InteractionHand.MAIN_HAND;
        if (player.getOffhandItem().getItem() instanceof BloodScytheItem) return InteractionHand.OFF_HAND;
        return null;
    }
}
