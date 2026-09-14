package com.shipovskijkorp.scythes.mod.effect;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.util.ScytheDamageTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * Keeps the vanilla freezing overlay active and applies the original
 * duration-based periodic damage used by the 1.20.1 implementation.
 */
public final class FreezingEffect extends MobEffect {

    public static final int DAMAGE_INTERVAL_TICKS = 20;
    public static final float DAMAGE_PER_PROC = 1.0F;
    public static final float DAMAGE_CHANCE = 0.60F;

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
                && instance.getDuration() % DAMAGE_INTERVAL_TICKS == 0
                && entity.getRandom().nextFloat() < DAMAGE_CHANCE) {
            entity.hurtServer(level, ScytheDamageTypes.freezing(level), DAMAGE_PER_PROC);
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
