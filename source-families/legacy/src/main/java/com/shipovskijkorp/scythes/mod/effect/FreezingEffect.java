package com.shipovskijkorp.scythes.mod.effect;

import com.shipovskijkorp.scythes.mod.ability.DamageAttributionTracker;
import com.shipovskijkorp.scythes.mod.ability.ScytheAdvancementTracker;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.util.ScytheDamageTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.SnowGolemEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

/**
 * Completely immobilizes a living entity and periodically attempts to deal
 * a fixed amount of true damage. Movement and look locking are enforced by
 * mixins so the effect works for both players and mobs.
 */
public final class FreezingEffect extends StatusEffect {

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
                || entity.getStatusEffect(this).getDuration() % ScytheBalance.Freezing.DAMAGE_INTERVAL_TICKS != 0
                || entity.getRandom().nextFloat() >= ScytheBalance.Freezing.DAMAGE_CHANCE) {
            return;
        }

        // Intentionally ignores amplifier: every level has the same damage.
        ServerPlayerEntity freezingOwner = entity instanceof SnowGolemEntity
                ? DamageAttributionTracker.getFreezingOwner(entity)
                : null;
        entity.damage(ScytheDamageTypes.freezing(entity.getWorld()), ScytheBalance.Freezing.DAMAGE_PER_PROC);
        if (!entity.isAlive() && freezingOwner != null) {
            ScytheAdvancementTracker.tryGrantSupercooledSnow(freezingOwner);
        }
    }
}
