package com.shipovskijkorp.scythes.mod.effect;

import com.shipovskijkorp.scythes.mod.ability.DamageAttributionTracker;
import com.shipovskijkorp.scythes.mod.util.ScytheDamageTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public class BleedingEffect extends StatusEffect {

    public static final int TICK_RATE = 20;
    public static final double DAMAGE_PER_SECOND = 1.5D;

    public BleedingEffect() {
        super(StatusEffectCategory.HARMFUL, 0x8B0000);
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return duration % TICK_RATE == 0;
    }

    @Override
    public boolean applyUpdateEffect(ServerWorld world, LivingEntity entity, int amplifier) {
        float damagePerProc = (float) (DAMAGE_PER_SECOND * (TICK_RATE / 20.0D));
        float damage = damagePerProc * (amplifier + 1);

        if (damage <= 0.0f) return true;

        ServerPlayerEntity owner = DamageAttributionTracker.getBleedingOwner(entity);
        if (owner != null) {
            entity.damage(world, ScytheDamageTypes.bleeding(world, owner), damage);
        } else {
            entity.damage(world, ScytheDamageTypes.bleeding(world), damage);
        }
        return true;
    }
}
