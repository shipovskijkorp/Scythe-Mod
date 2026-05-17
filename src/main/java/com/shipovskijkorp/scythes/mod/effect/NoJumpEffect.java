package com.shipovskijkorp.scythes.mod.effect;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

public class NoJumpEffect extends StatusEffect {

    public NoJumpEffect() {
        super(StatusEffectCategory.HARMFUL, 0x3A3A3A);
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return false;
    }
}
