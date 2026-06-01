package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.config.ScytheModConfig;
import com.shipovskijkorp.scythes.mod.config.ScytheModConfigLoader;
import com.shipovskijkorp.scythes.mod.item.WitheringScytheItem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;

import java.util.List;

public class WitheringScytheAura {

    public static void apply(ServerPlayerEntity player) {

        boolean hasScytheInHand =
                player.getMainHandStack().getItem() instanceof WitheringScytheItem
                        || player.getOffHandStack().getItem() instanceof WitheringScytheItem;

        if (!hasScytheInHand) return;

        ScytheModConfig config = ScytheModConfigLoader.getConfig();
        double radius = config.witheringAuraRadius;
        int witherAdd = Math.max(1, config.witheringAuraWitherTicks);
        int slowAdd = Math.max(1, config.witheringAuraSlownessTicks);
        int threshold = Math.max(1, config.witheringAuraRefreshThresholdTicks);

        Box box = player.getBoundingBox().expand(radius);
        List<LivingEntity> targets =
                player.getWorld().getEntitiesByClass(
                        LivingEntity.class,
                        box,
                        target -> ScytheTargeting.canHit(player, target)
                );

        for (LivingEntity target : targets) {
            int witherDuration = extendOrApply(target, StatusEffects.WITHER, witherAdd, 0, threshold);
            if (witherDuration > 0) {
                DamageAttributionTracker.recordWithering(target, player, witherDuration);
            }
            extendOrApply(target, StatusEffects.SLOWNESS, slowAdd, 0, threshold);
        }
    }

    private static int extendOrApply(LivingEntity target,
                                     StatusEffect effect,
                                     int addTicks,
                                     int amplifier,
                                     int thresholdTicks) {

        StatusEffectInstance cur = target.getStatusEffect(effect);

        if (cur == null) {
            target.addStatusEffect(new StatusEffectInstance(effect, addTicks, amplifier));
            return addTicks;
        }

        if (cur.getDuration() <= thresholdTicks) {
            int newDuration = cur.getDuration() + addTicks;
            target.addStatusEffect(new StatusEffectInstance(
                    effect,
                    newDuration,
                    Math.max(cur.getAmplifier(), amplifier)
            ));
            return newDuration;
        }

        return 0;
    }
}
