package com.shipovskijkorp.scythes.mod.effect;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.ability.DamageAttributionTracker;
import com.shipovskijkorp.scythes.mod.ability.ScytheAdvancementTracker;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.util.ScytheDamageTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.SnowGolemEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;

/** Completely immobilizes a target and periodically deals fixed true damage. */
public final class FreezingEffect extends StatusEffect {

    public FreezingEffect() {
        super(StatusEffectCategory.HARMFUL, 0x8FD8F4);
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyUpdateEffect(LivingEntity entity, int amplifier) {
        if (entity.getWorld().isClient || !entity.isAlive()) {
            return true;
        }

        int visualFreezeTicks = Math.max(0, entity.getMinFreezeDamageTicks() - 1);
        if (entity.getFrozenTicks() < visualFreezeTicks) {
            entity.setFrozenTicks(visualFreezeTicks);
        }

        StatusEffectInstance instance = entity.getStatusEffect(ScytheMod.FREEZING);
        if (instance != null
                && instance.getDuration() % ScytheBalance.Freezing.DAMAGE_INTERVAL_TICKS == 0
                && entity.getRandom().nextFloat() < ScytheBalance.Freezing.DAMAGE_CHANCE) {
            ServerPlayerEntity freezingOwner = entity instanceof SnowGolemEntity
                    ? DamageAttributionTracker.getFreezingOwner(entity)
                    : null;
            entity.damage(ScytheDamageTypes.freezing(entity.getWorld()), ScytheBalance.Freezing.DAMAGE_PER_PROC);
            if (!entity.isAlive() && freezingOwner != null) {
                ScytheAdvancementTracker.tryGrantSupercooledSnow(freezingOwner);
            }
        }

        return true;
    }
}
