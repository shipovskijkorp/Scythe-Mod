package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.item.FrozenScytheItem;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;

import java.util.List;

/** Active ability of the Frost Scythe. */
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

    public static void tryActivate(ServerPlayerEntity player) {
        Hand hand = FrozenScytheItem.getHeldFrozenScytheHand(player);
        if (hand == null) {
            player.sendMessage(Text.translatable("message.scythes.frozen_storm.no_scythe"), true);
            return;
        }

        int cooldownLeft = FrozenScytheCooldowns.getStormTicksLeft(player);
        if (cooldownLeft > 0) {
            player.sendMessage(
                    Text.translatable("message.scythes.frozen_storm.cooldown", Math.max(1, (cooldownLeft + 19) / 20)),
                    true
            );
            return;
        }

        ItemStack stack = player.getStackInHand(hand);
        if (!FrozenScytheItem.hasEnoughDurability(stack, DURABILITY_COST)) {
            player.sendMessage(Text.translatable("message.scythes.scythe_ability.no_durability"), true);
            return;
        }

        ServerWorld world = player.getEntityWorld();
        double radiusSquared = RADIUS * RADIUS;
        Box searchBox = player.getBoundingBox().expand(RADIUS);
        List<LivingEntity> targets = world.getEntitiesByClass(
                LivingEntity.class,
                searchBox,
                target -> isValidTarget(player, target)
                        && target.squaredDistanceTo(player) <= radiusSquared
        );

        if (targets.isEmpty()) {
            player.sendMessage(Text.translatable("message.scythes.frozen_storm.no_targets"), true);
            return;
        }

        for (LivingEntity target : targets) {
            target.addStatusEffect(new StatusEffectInstance(
                    ScytheMod.FREEZING,
                    FREEZING_TICKS,
                    0,
                    false,
                    true,
                    true
            ));
            target.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.SLOWNESS,
                    SLOWNESS_TICKS,
                    SLOWNESS_AMPLIFIER,
                    false,
                    true,
                    true
            ));
            target.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.WEAKNESS,
                    WEAKNESS_TICKS,
                    WEAKNESS_AMPLIFIER,
                    false,
                    true,
                    true
            ));
        }

        stack.damage(
                DURABILITY_COST,
                player,
                hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND
        );
        FrozenScytheCooldowns.setStormCooldown(player, COOLDOWN_TICKS);
        ScytheAdvancementTracker.markFrozenActive(player);

        world.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ENTITY_EVOKER_CAST_SPELL,
                SoundCategory.PLAYERS,
                1.25F,
                0.65F
        );
        player.sendMessage(Text.translatable("message.scythes.frozen_storm.success", targets.size()), true);
    }

    private static boolean isValidTarget(ServerPlayerEntity player, LivingEntity target) {
        if (target == player) return false;
        if (!target.isAlive()) return false;
        if (target.isSpectator()) return false;
        if (player.isTeammate(target)) return false;
        if (target instanceof TameableEntity tameable && tameable.isTamed()) return false;
        return !(target instanceof AbstractHorseEntity horse) || !horse.isTame();
    }
}
