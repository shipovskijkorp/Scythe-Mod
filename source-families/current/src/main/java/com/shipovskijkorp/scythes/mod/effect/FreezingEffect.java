package com.shipovskijkorp.scythes.mod.effect;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.ability.DamageAttributionTracker;
import com.shipovskijkorp.scythes.mod.ability.ScytheAdvancementTracker;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.util.ScytheDamageTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.SnowGolem;

/**
 * Keeps the vanilla freezing overlay active and applies the original
 * duration-based periodic damage used by the 1.20.1 implementation.
 */
public final class FreezingEffect extends MobEffect {

    public FreezingEffect() {
        super(MobEffectCategory.HARMFUL, 0x8FD8F4);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (!entity.isAlive()) {
            return true;
        }

        MobEffectInstance instance = entity.getEffect(ScytheMod.FREEZING);
        if (instance != null
                && instance.getDuration() % ScytheBalance.Freezing.DAMAGE_INTERVAL_TICKS == 0
                && entity.getRandom().nextFloat() < ScytheBalance.Freezing.DAMAGE_CHANCE) {
            ServerPlayer freezingOwner = entity instanceof SnowGolem
                    ? DamageAttributionTracker.getFreezingOwner(entity)
                    : null;
            entity.hurtServer(level, ScytheDamageTypes.freezing(level), ScytheBalance.Freezing.DAMAGE_PER_PROC);
            if (!entity.isAlive() && freezingOwner != null) {
                ScytheAdvancementTracker.tryGrantSupercooledSnow(freezingOwner);
            }
        }

        // One tick below the vanilla damage threshold preserves the frosted HUD
        // without stacking vanilla powder-snow damage with the custom damage.
        int visualFreezeTicks = Math.max(0, entity.getTicksRequiredToFreeze() - 1);
        if (entity.getTicksFrozen() < visualFreezeTicks) {
            entity.setTicksFrozen(visualFreezeTicks);
        }
        return true;
    }
}
