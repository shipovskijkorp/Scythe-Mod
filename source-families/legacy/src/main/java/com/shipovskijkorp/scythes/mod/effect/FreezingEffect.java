package com.shipovskijkorp.scythes.mod.effect;

import com.shipovskijkorp.scythes.mod.util.ScytheDamageTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

/**
 * Completely immobilizes a living entity and periodically attempts to deal
 * a fixed amount of true damage. Movement and look locking are enforced by
 * mixins so the effect works for both players and mobs.
 */
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
    public void applyUpdateEffect(LivingEntity entity, int amplifier) {
        if (entity.getWorld().isClient || !entity.isAlive()) return;

        // Keep the vanilla powder-snow HUD fully frosted for the whole effect.
        // One tick below the damage threshold preserves the visual without
        // adding vanilla powder-snow damage on top of this effect's own damage.
        int visualFreezeTicks = Math.max(0, entity.getMinFreezeDamageTicks() - 1);
        if (entity.getFrozenTicks() < visualFreezeTicks) {
            entity.setFrozenTicks(visualFreezeTicks);
        }

        if (entity.getStatusEffect(this) == null
                || entity.getStatusEffect(this).getDuration() % DAMAGE_INTERVAL_TICKS != 0
                || entity.getRandom().nextFloat() >= DAMAGE_CHANCE) {
            return;
        }

        // Intentionally ignores amplifier: every level has the same damage.
        entity.damage(ScytheDamageTypes.freezing(entity.getWorld()), DAMAGE_PER_PROC);
    }
}
