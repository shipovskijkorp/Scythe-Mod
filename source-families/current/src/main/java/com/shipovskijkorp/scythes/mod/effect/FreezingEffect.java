package com.shipovskijkorp.scythes.mod.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/** Keeps the vanilla freezing overlay active; periodic damage is ticked by the living-entity mixin. */
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
        if (!entity.isAlive()) return true;
        int visualFreezeTicks = Math.max(0, entity.getTicksRequiredToFreeze() - 1);
        if (entity.getTicksFrozen() < visualFreezeTicks) entity.setTicksFrozen(visualFreezeTicks);
        return true;
    }
}
