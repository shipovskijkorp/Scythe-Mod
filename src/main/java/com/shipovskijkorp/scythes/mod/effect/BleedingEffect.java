package com.shipovskijkorp.scythes.mod.effect;

import com.shipovskijkorp.scythes.mod.ability.DamageAttributionTracker;
import com.shipovskijkorp.scythes.mod.config.ScytheModConfigLoader;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

public class BleedingEffect extends StatusEffect {

    public BleedingEffect() {
        super(StatusEffectCategory.HARMFUL, 0x8B0000);
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        int tickRate = 20;
        if (ScytheModConfigLoader.CONFIG != null) {
            tickRate = Math.max(1, ScytheModConfigLoader.CONFIG.bleedingTickRate);
        }
        return duration % tickRate == 0;
    }

    @Override
    public void applyUpdateEffect(LivingEntity entity, int amplifier) {
        World world = entity.getWorld();
        if (world.isClient) return;

        int tickRate = 20;
        double dps = 1.5;

        if (ScytheModConfigLoader.CONFIG != null) {
            tickRate = Math.max(1, ScytheModConfigLoader.CONFIG.bleedingTickRate);
            dps = Math.max(0.0, ScytheModConfigLoader.CONFIG.bleedingDamagePerSecond);
        }

        float damagePerProc = (float) (dps * (tickRate / 20.0));
        float damage = damagePerProc * (amplifier + 1);

        if (damage <= 0.0f) return;

        ServerPlayerEntity owner = DamageAttributionTracker.getBleedingOwner(entity);
        if (owner != null) {
            entity.damage(entity.getDamageSources().indirectMagic(owner, owner), damage);
        } else {
            entity.damage(world.getDamageSources().magic(), damage);
        }
    }
}
