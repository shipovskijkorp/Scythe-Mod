package com.shipovskijkorp.scythes.mod.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Marker effect. Healing suppression is enforced by LivingEntityMixin. */
public final class BurnsEffect extends MobEffect {
    public BurnsEffect() {
        super(MobEffectCategory.HARMFUL, 0xE85A19);
    }
}
