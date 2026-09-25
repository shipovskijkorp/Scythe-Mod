package com.shipovskijkorp.scythes.mod.mixin;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.ability.DamageAttributionTracker;
import com.shipovskijkorp.scythes.mod.ability.ScytheAdvancementTracker;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.util.ScytheDamageTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.SnowGolemEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class FreezingLivingEntityMixin {

    @Unique private boolean scythes$wasFrozen;
    @Unique private int scythes$freezingDamageTicks;
    @Unique private float scythes$frozenYaw;
    @Unique private float scythes$frozenPitch;
    @Unique private float scythes$frozenBodyYaw;
    @Unique private float scythes$frozenHeadYaw;

    @Inject(method = "baseTick", at = @At("TAIL"))
    private void scythes$tickFreezingState(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self.getEntityWorld() instanceof ServerWorld world)) return;
        boolean frozen = self.isAlive() && self.hasStatusEffect(ScytheMod.FREEZING);

        if (!frozen) {
            scythes$freezingDamageTicks = 0;
            return;
        }

        scythes$freezingDamageTicks++;
        if (scythes$freezingDamageTicks < ScytheBalance.Freezing.DAMAGE_INTERVAL_TICKS) {
            return;
        }

        scythes$freezingDamageTicks = 0;
        if (self.getRandom().nextFloat() < ScytheBalance.Freezing.DAMAGE_CHANCE) {
            ServerPlayerEntity freezingOwner = DamageAttributionTracker.getFreezingOwner(self);
            self.damage(
                    world,
                    freezingOwner != null ? ScytheDamageTypes.freezing(world, freezingOwner) : ScytheDamageTypes.freezing(world),
                    ScytheBalance.Freezing.DAMAGE_PER_PROC
            );
            if (!self.isAlive() && self instanceof SnowGolemEntity && freezingOwner != null) {
                ScytheAdvancementTracker.tryGrantSupercooledSnow(freezingOwner);
            }
        }
    }

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void scythes$cancelTravelWhileFrozen(Vec3d movementInput, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.hasStatusEffect(ScytheMod.FREEZING)) return;

        self.setVelocity(Vec3d.ZERO);
        ci.cancel();
    }

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void scythes$lockFrozenEntityBeforeMovement(CallbackInfo ci) {
        scythes$applyFrozenLock();
    }

    @Inject(method = "tickMovement", at = @At("TAIL"))
    private void scythes$lockFrozenEntityAfterMovement(CallbackInfo ci) {
        scythes$applyFrozenLock();
    }

    @Unique
    private void scythes$applyFrozenLock() {
        LivingEntity self = (LivingEntity) (Object) this;

        if (!self.hasStatusEffect(ScytheMod.FREEZING)) {
            scythes$wasFrozen = false;
            return;
        }

        if (!scythes$wasFrozen) {
            scythes$frozenYaw = self.getYaw();
            scythes$frozenPitch = self.getPitch();
            scythes$frozenBodyYaw = self.getBodyYaw();
            scythes$frozenHeadYaw = self.getHeadYaw();
            scythes$wasFrozen = true;
        }

        self.setVelocity(Vec3d.ZERO);
        self.setYaw(scythes$frozenYaw);
        self.setPitch(scythes$frozenPitch);
        self.setBodyYaw(scythes$frozenBodyYaw);
        self.setHeadYaw(scythes$frozenHeadYaw);
        self.lastYaw = scythes$frozenYaw;
        self.lastPitch = scythes$frozenPitch;
        self.lastBodyYaw = scythes$frozenBodyYaw;
        self.lastHeadYaw = scythes$frozenHeadYaw;
    }
}
