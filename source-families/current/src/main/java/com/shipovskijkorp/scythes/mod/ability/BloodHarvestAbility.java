package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.util.ScytheCombatUtil;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.item.BloodScytheItem;
import com.shipovskijkorp.scythes.mod.platform.HudSync;
import com.shipovskijkorp.scythes.mod.platform.HudTransport;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

public class BloodHarvestAbility {

    /** Активация способности */
    public static void tryActivate(ServerPlayer player) {

        InteractionHand hand = getHeldBloodScytheHand(player);
        if (hand == null) {
            player.sendOverlayMessage(Component.translatable("message.scythes.blood_harvest.no_scythe"));
            return;
        }

        ItemStack stack = player.getItemInHand(hand);
        ServerLevel world = (ServerLevel) player.level();

        if (ScytheBalance.BloodHarvest.DURABILITY_COST > 0) {
            int remaining = stack.getMaxDamage() - stack.getDamageValue();
            if (remaining < ScytheBalance.BloodHarvest.DURABILITY_COST) {
                player.sendOverlayMessage(Component.translatable("message.scythes.blood_harvest.no_durability"));
                return;
            }
        }

        if (ScytheCooldowns.remaining(player, ScytheCooldowns.Skill.BLOOD_HARVEST) > 0
                || player.getCooldowns().isOnCooldown(stack)) {
            player.sendOverlayMessage(Component.translatable("message.scythes.blood_harvest.cooldown"));
            return;
        }

        AABB box = player.getBoundingBox().inflate(ScytheBalance.BloodHarvest.RADIUS);
        List<ServerPlayer> targets =
                world.getEntitiesOfClass(
                        ServerPlayer.class,
                        box,
                        p -> p != player && p.isAlive() && !p.isSpectator() && !player.isAlliedTo(p)
                                && ScytheCombatUtil.isWithinRadius(player, p, ScytheBalance.BloodHarvest.RADIUS)
                );

        if (targets.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.scythes.blood_harvest.no_targets"));
            return;
        }

        // ✅ тратим прочность ТОЛЬКО на активированной косе
        if (ScytheBalance.BloodHarvest.DURABILITY_COST > 0) {
            stack.hurtAndBreak(ScytheBalance.BloodHarvest.DURABILITY_COST, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }

        player.getCooldowns().addCooldown(stack, ScytheBalance.BloodHarvest.COOLDOWN_TICKS);
        ScytheCooldowns.start(player, ScytheCooldowns.Skill.BLOOD_HARVEST, ScytheBalance.BloodHarvest.COOLDOWN_TICKS);

        for (ServerPlayer target : targets) {
            ScytheAdvancementTracker.markBloodActive(player, target);
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ScytheBalance.BloodHarvest.SLOWNESS_TICKS, ScytheBalance.BloodHarvest.SLOWNESS_AMPLIFIER));
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ScytheBalance.BloodHarvest.BLINDNESS_TICKS, ScytheBalance.BloodHarvest.BLINDNESS_AMPLIFIER));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ScytheBalance.BloodHarvest.WEAKNESS_TICKS, ScytheBalance.BloodHarvest.WEAKNESS_AMPLIFIER));
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, ScytheBalance.BloodHarvest.GLOWING_TICKS, ScytheBalance.BloodHarvest.GLOWING_AMPLIFIER));
        }

        int windowTicks = BloodHarvestTracker.start(
                player,
                targets.stream().map(ServerPlayer::getUUID).toList()
        );
        HudSync.start(player, HudTransport.Timer.BLOOD_HARVEST, windowTicks);

        player.sendOverlayMessage(Component.translatable("message.scythes.blood_harvest.success"));
    }

    private static InteractionHand getHeldBloodScytheHand(ServerPlayer player) {
        if (player.getMainHandItem().getItem() instanceof BloodScytheItem) return InteractionHand.MAIN_HAND;
        if (player.getOffhandItem().getItem() instanceof BloodScytheItem) return InteractionHand.OFF_HAND;
        return null;
    }
}
