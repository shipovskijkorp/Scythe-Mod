package com.shipovskijkorp.scythes.mod.effect;

import com.shipovskijkorp.scythes.mod.ability.DamageAttributionTracker;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.util.ScytheDamageTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class BleedingEffect extends MobEffect {

    public BleedingEffect() {
        super(MobEffectCategory.HARMFUL, 0x8B0000);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % ScytheBalance.Bleeding.TICK_RATE == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel world, LivingEntity entity, int amplifier) {
        float damagePerProc = (float) (ScytheBalance.Bleeding.DAMAGE_PER_SECOND * (ScytheBalance.Bleeding.TICK_RATE / (double) ScytheBalance.TICKS_PER_SECOND));
        float damage = damagePerProc * (amplifier + 1);

        if (damage <= 0.0f) return true;

        ServerPlayer owner = DamageAttributionTracker.getBleedingOwner(entity);
        if (owner != null) {
            entity.hurtServer(world, ScytheDamageTypes.bleeding(world, owner), damage);
        } else {
            entity.hurtServer(world, ScytheDamageTypes.bleeding(world), damage);
        }
        return true;
    }
}
