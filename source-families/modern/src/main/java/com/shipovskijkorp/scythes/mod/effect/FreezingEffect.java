package com.shipovskijkorp.scythes.mod.effect;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.util.ScytheDamageTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;

/** Completely immobilizes a target and periodically deals fixed true damage. */
public final class FreezingEffect extends StatusEffect {

    public static final int DAMAGE_INTERVAL_TICKS = 20;
    public static final float DAMAGE_PER_PROC = 1.0F;
    public static final float DAMAGE_CHANCE = 0.60F;

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
                && instance.getDuration() % DAMAGE_INTERVAL_TICKS == 0
                && entity.getRandom().nextFloat() < DAMAGE_CHANCE) {
            entity.damage(ScytheDamageTypes.freezing(entity.getWorld()), DAMAGE_PER_PROC);
        }

        return true;
    }
}
