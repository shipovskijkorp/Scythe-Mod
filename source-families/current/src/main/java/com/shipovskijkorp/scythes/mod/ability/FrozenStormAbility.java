package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.item.FrozenScytheItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** Active ability of the Frozen Scythe. */
public final class FrozenStormAbility {

    public static final double RADIUS = 20.0D;
    public static final int COOLDOWN_TICKS = 20 * 40;
    public static final int DURABILITY_COST = 100;

    public static final int FREEZING_TICKS = 20 * 5;
    public static final int SLOWNESS_TICKS = 20 * 10;
    public static final int WEAKNESS_TICKS = 20 * 10;
    public static final int SLOWNESS_AMPLIFIER = 0;
    public static final int WEAKNESS_AMPLIFIER = 0;

    private FrozenStormAbility() {
    }

    public static void tryActivate(ServerPlayer player) {
        InteractionHand hand = FrozenScytheItem.getHeldFrozenScytheHand(player);
        if (hand == null) {
            player.sendOverlayMessage(Component.translatable("message.scythes.frozen_storm.no_scythe"));
            return;
        }

        int cooldownLeft = FrozenScytheCooldowns.getStormTicksLeft(player);
        if (cooldownLeft > 0) {
            player.sendOverlayMessage(Component.translatable(
                    "message.scythes.frozen_storm.cooldown",
                    Math.max(1, (cooldownLeft + 19) / 20)
            ));
            return;
        }

        ItemStack stack = player.getItemInHand(hand);
        if (!FrozenScytheItem.hasEnoughDurability(stack, DURABILITY_COST)) {
            player.sendOverlayMessage(Component.translatable("message.scythes.scythe_ability.no_durability"));
            return;
        }

        ServerLevel level = (ServerLevel) player.level();
        double radiusSquared = RADIUS * RADIUS;
        AABB searchBox = player.getBoundingBox().inflate(RADIUS);
        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class,
                searchBox,
                target -> isValidTarget(player, target) && target.distanceToSqr(player) <= radiusSquared
        );

        if (targets.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.scythes.frozen_storm.no_targets"));
            return;
        }

        for (LivingEntity target : targets) {
            target.addEffect(new MobEffectInstance(ScytheMod.FREEZING, FREEZING_TICKS, 0, false, true, true));
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, SLOWNESS_TICKS, SLOWNESS_AMPLIFIER, false, true, true));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, WEAKNESS_TICKS, WEAKNESS_AMPLIFIER, false, true, true));
        }

        stack.hurtAndBreak(
                DURABILITY_COST,
                player,
                hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND
        );
        FrozenScytheCooldowns.setStormCooldown(player, COOLDOWN_TICKS);
        ScytheAdvancementTracker.markFrozenActive(player);

        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.EVOKER_CAST_SPELL,
                SoundSource.PLAYERS,
                1.25F,
                0.65F
        );
        player.sendOverlayMessage(Component.translatable("message.scythes.frozen_storm.success", targets.size()));
    }

    private static boolean isValidTarget(ServerPlayer player, LivingEntity target) {
        if (target == player) return false;
        if (!target.isAlive()) return false;
        if (target.isSpectator()) return false;
        if (player.isAlliedTo(target)) return false;
        if (target instanceof TamableAnimal tameable && tameable.isTame()) return false;
        return !(target instanceof AbstractHorse horse) || !horse.isTamed();
    }
}
