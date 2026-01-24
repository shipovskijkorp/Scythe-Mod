package com.example.bloodyscythe.effect;

import com.example.bloodyscythe.config.BloodyScytheConfigLoader;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.world.World;

public class BleedingEffect extends StatusEffect {

    public BleedingEffect() {
        super(StatusEffectCategory.HARMFUL, 0x8B0000);
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        int tickRate = 20;
        if (BloodyScytheConfigLoader.CONFIG != null) {
            tickRate = Math.max(1, BloodyScytheConfigLoader.CONFIG.bleedingTickRate);
        }
        return duration % tickRate == 0;
    }

    @Override
    public void applyUpdateEffect(LivingEntity entity, int amplifier) {
        World world = entity.getWorld();
        if (world.isClient) return;

        int tickRate = 20;
        double dps = 1.5;

        if (BloodyScytheConfigLoader.CONFIG != null) {
            tickRate = Math.max(1, BloodyScytheConfigLoader.CONFIG.bleedingTickRate);
            dps = Math.max(0.0, BloodyScytheConfigLoader.CONFIG.bleedingDamagePerSecond);
        }

        // сохраняем DPS при любом tickRate
        float damagePerProc = (float) (dps * (tickRate / 20.0));
        float damage = damagePerProc * (amplifier + 1);

        if (damage > 0.0f) {
            entity.damage(world.getDamageSources().magic(), damage);
        }
    }
}
