package com.shipovskijkorp.scythes.mod.mixin;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.util.FreezingRenderState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class FreezingLivingEntityMixin implements FreezingRenderState {

    @Unique
    private static final TrackedData<Boolean> SCYTHES_FROZEN_FOR_RENDERING =
            DataTracker.registerData(LivingEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    @Unique
    private boolean scythes$wasFrozen;

    @Unique
    private float scythes$frozenYaw;

    @Unique
    private float scythes$frozenPitch;

    @Unique
    private float scythes$frozenBodyYaw;

    @Unique
    private float scythes$frozenHeadYaw;

    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void scythes$startTrackingFrozenRenderState(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        self.getDataTracker().startTracking(SCYTHES_FROZEN_FOR_RENDERING, false);
    }

    @Inject(method = "baseTick", at = @At("TAIL"))
    private void scythes$syncFrozenRenderState(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.getWorld().isClient) return;

        boolean frozen = self.hasStatusEffect(ScytheMod.FREEZING);
        if (self.getDataTracker().get(SCYTHES_FROZEN_FOR_RENDERING) != frozen) {
            self.getDataTracker().set(SCYTHES_FROZEN_FOR_RENDERING, frozen);
        }
    }

    @Override
    public boolean scythes$isFrozenForRendering() {
        LivingEntity self = (LivingEntity) (Object) this;
        return self.getDataTracker().get(SCYTHES_FROZEN_FOR_RENDERING);
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

        // Keep interpolation from visually rotating the frozen target.
        self.prevYaw = scythes$frozenYaw;
        self.prevPitch = scythes$frozenPitch;
        self.prevBodyYaw = scythes$frozenBodyYaw;
        self.prevHeadYaw = scythes$frozenHeadYaw;
    }
}
