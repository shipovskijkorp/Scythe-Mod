package com.shipovskijkorp.scythes.mod.effect;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

/** Marker effect. Healing suppression is enforced by LivingEntityMixin. */
public final class BurnsEffect extends StatusEffect {
    public BurnsEffect() {
        super(StatusEffectCategory.HARMFUL, 0xE85A19);
    }
}
