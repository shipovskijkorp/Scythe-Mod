package com.shipovskijkorp.scythes.mod.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class NoJumpEffect extends MobEffect {

    public NoJumpEffect() {
        super(MobEffectCategory.HARMFUL, 0x3A3A3A);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return false;
    }
}
