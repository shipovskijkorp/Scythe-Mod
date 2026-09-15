package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.util.ScytheCombatUtil;
import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.item.FrozenScytheItem;
import java.util.List;
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

/** Active ability of the Frozen Scythe. */
public final class FrozenStormAbility {

    private FrozenStormAbility() {
    }

    public static void tryActivate(ServerPlayer player) {
        InteractionHand hand = FrozenScytheItem.getHeldFrozenScytheHand(player);
        if (hand == null) {
            player.sendOverlayMessage(Component.translatable("message.scythes.frozen_storm.no_scythe"));
            return;
        }

        int cooldownLeft = ScytheCooldowns.remaining(player, ScytheCooldowns.Skill.STORM);
        if (cooldownLeft > 0) {
            player.sendOverlayMessage(Component.translatable(
                    "message.scythes.frozen_storm.cooldown",
                    Math.max(1, (cooldownLeft + 19) / 20)
            ));
            return;
        }

        ItemStack stack = player.getItemInHand(hand);
        if (!FrozenScytheItem.hasEnoughDurability(stack, ScytheBalance.FrozenStorm.DURABILITY_COST)) {
            player.sendOverlayMessage(Component.translatable("message.scythes.scythe_ability.no_durability"));
            return;
        }

        ServerLevel level = (ServerLevel) player.level();
        double radiusSquared = ScytheBalance.FrozenStorm.RADIUS * ScytheBalance.FrozenStorm.RADIUS;
        AABB searchBox = player.getBoundingBox().inflate(ScytheBalance.FrozenStorm.RADIUS);
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
            target.addEffect(new MobEffectInstance(ScytheMod.FREEZING, ScytheBalance.FrozenStorm.FREEZING_TICKS, ScytheBalance.FrozenStorm.FREEZING_AMPLIFIER, false, true, true));
            DamageAttributionTracker.recordFreezing(target, player, ScytheBalance.FrozenStorm.FREEZING_TICKS);
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ScytheBalance.FrozenStorm.SLOWNESS_TICKS, ScytheBalance.FrozenStorm.SLOWNESS_AMPLIFIER, false, true, true));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ScytheBalance.FrozenStorm.WEAKNESS_TICKS, ScytheBalance.FrozenStorm.WEAKNESS_AMPLIFIER, false, true, true));
        }

        stack.hurtAndBreak(
                ScytheBalance.FrozenStorm.DURABILITY_COST,
                player,
                hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND
        );
        ScytheCooldowns.start(player, ScytheCooldowns.Skill.STORM, ScytheBalance.FrozenStorm.COOLDOWN_TICKS);
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
        if (ScytheCombatUtil.isProtectedWitheringMinion(player, target)) return false;
        if (target == player) return false;
        if (!target.isAlive()) return false;
        if (target.isSpectator()) return false;
        if (player.isAlliedTo(target)) return false;
        if (target instanceof TamableAnimal tameable && tameable.isTame()) return false;
        return !(target instanceof AbstractHorse horse) || !horse.isTamed();
    }
}
