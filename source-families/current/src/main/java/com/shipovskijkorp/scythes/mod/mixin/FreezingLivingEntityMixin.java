package com.shipovskijkorp.scythes.mod.mixin;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.util.FreezingRenderState;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class FreezingLivingEntityMixin implements FreezingRenderState {

    @Unique
    private static final EntityDataAccessor<Boolean> SCYTHES_FROZEN_FOR_RENDERING =
            SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.BOOLEAN);

    @Unique private boolean scythes$wasFrozen;
    @Unique private float scythes$frozenYaw;
    @Unique private float scythes$frozenPitch;
    @Unique private float scythes$frozenBodyYaw;
    @Unique private float scythes$frozenHeadYaw;

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void scythes$defineFrozenState(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(SCYTHES_FROZEN_FOR_RENDERING, false);
    }

    @Inject(method = "baseTick", at = @At("TAIL"))
    private void scythes$tickFreezing(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        boolean frozen = self.isAlive() && self.hasEffect(ScytheMod.FREEZING);

        if (!self.level().isClientSide()) {
            if (self.getEntityData().get(SCYTHES_FROZEN_FOR_RENDERING) != frozen) {
                self.getEntityData().set(SCYTHES_FROZEN_FOR_RENDERING, frozen);
            }
        }
    }

    @Override
    public boolean scythes$isFrozenForRendering() {
        LivingEntity self = (LivingEntity) (Object) this;
        return self.getEntityData().get(SCYTHES_FROZEN_FOR_RENDERING);
    }

    @Override
    public void scythes$setFrozenForRendering(boolean frozen) {
        LivingEntity self = (LivingEntity) (Object) this;
        self.getEntityData().set(SCYTHES_FROZEN_FOR_RENDERING, frozen);
    }

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void scythes$cancelTravelWhileFrozen(Vec3 movementInput, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.hasEffect(ScytheMod.FREEZING)) return;
        self.setDeltaMovement(Vec3.ZERO);
        ci.cancel();
    }

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void scythes$lockBeforeMovement(CallbackInfo ci) {
        scythes$applyFrozenLock();
    }

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void scythes$lockAfterMovement(CallbackInfo ci) {
        scythes$applyFrozenLock();
    }

    @Unique
    private void scythes$applyFrozenLock() {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.hasEffect(ScytheMod.FREEZING)) {
            scythes$wasFrozen = false;
            return;
        }

        if (!scythes$wasFrozen) {
            scythes$frozenYaw = self.getYRot();
            scythes$frozenPitch = self.getXRot();
            scythes$frozenBodyYaw = self.yBodyRot;
            scythes$frozenHeadYaw = self.getYHeadRot();
            scythes$wasFrozen = true;
        }

        self.setDeltaMovement(Vec3.ZERO);
        self.setYRot(scythes$frozenYaw);
        self.setXRot(scythes$frozenPitch);
        self.setYBodyRot(scythes$frozenBodyYaw);
        self.setYHeadRot(scythes$frozenHeadYaw);
    }
}
