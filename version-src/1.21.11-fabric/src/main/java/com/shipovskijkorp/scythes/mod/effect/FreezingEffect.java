package com.shipovskijkorp.scythes.mod.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.server.world.ServerWorld;

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
        // The vanilla frozen-ticks value must be refreshed every tick; otherwise
        // the cold overlay immediately starts fading outside powder snow.
        return true;
    }

    @Override
    public boolean applyUpdateEffect(ServerWorld world, LivingEntity entity, int amplifier) {
        if (!entity.isAlive()) {
            return true;
        }

        // Keep the vanilla powder-snow HUD fully frosted for the whole effect.
        // One tick below the damage threshold preserves the visual without
        // adding vanilla powder-snow damage on top of this effect's own damage.
        int visualFreezeTicks = Math.max(0, entity.getMinFreezeDamageTicks() - 1);
        if (entity.getFrozenTicks() < visualFreezeTicks) {
            entity.setFrozenTicks(visualFreezeTicks);
        }

        // Damage is ticked from FreezingLivingEntityMixin. A dedicated timer is
        // used there instead of the remaining effect duration so refreshed,
        // short and infinite instances all deal damage consistently.
        return true;
    }
}
