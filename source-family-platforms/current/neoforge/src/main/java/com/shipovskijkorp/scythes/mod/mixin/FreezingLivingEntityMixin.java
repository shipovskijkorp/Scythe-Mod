package com.shipovskijkorp.scythes.mod.mixin;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.ability.DamageAttributionTracker;
import com.shipovskijkorp.scythes.mod.ability.ScytheAdvancementTracker;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.util.ScytheDamageTypes;
import com.shipovskijkorp.scythes.mod.network.ModPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** NeoForge-specific freezing logic without foreign SynchedEntityData. */
@Mixin(LivingEntity.class)
public abstract class FreezingLivingEntityMixin {
    @Unique private boolean scythes$wasFrozen;
    @Unique private int scythes$freezingDamageTicks;
    @Unique private float scythes$frozenYaw;
    @Unique private float scythes$frozenPitch;
    @Unique private float scythes$frozenBodyYaw;
    @Unique private float scythes$frozenHeadYaw;
    @Unique private boolean scythes$visualStateInitialized;
    @Unique private boolean scythes$lastVisualFrozen;

    @Inject(method = "baseTick", at = @At("TAIL"))
    private void scythes$syncFreezingVisualState(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) return;

        boolean frozen = self.isAlive() && self.hasEffect(ScytheMod.FREEZING);
        boolean sendVisualUpdate = false;
        if (!scythes$visualStateInitialized) {
            scythes$visualStateInitialized = true;
            scythes$lastVisualFrozen = frozen;
            sendVisualUpdate = frozen;
        } else if (frozen != scythes$lastVisualFrozen) {
            scythes$lastVisualFrozen = frozen;
            sendVisualUpdate = true;
        }
        if (sendVisualUpdate) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                    self,
                    new ModPackets.FreezingVisualPayload(self.getUUID(), frozen)
            );
        }

        if (!frozen) {
            scythes$freezingDamageTicks = 0;
            return;
        }
        if (++scythes$freezingDamageTicks < ScytheBalance.Freezing.DAMAGE_INTERVAL_TICKS) return;
        scythes$freezingDamageTicks = 0;
        if (self.getRandom().nextFloat() >= ScytheBalance.Freezing.DAMAGE_CHANCE) return;
        if (!(self.level() instanceof ServerLevel level)) return;

        ServerPlayer freezingOwner = DamageAttributionTracker.getFreezingOwner(self);
        self.hurtServer(
                level,
                freezingOwner != null ? ScytheDamageTypes.freezing(level, freezingOwner) : ScytheDamageTypes.freezing(level),
                ScytheBalance.Freezing.DAMAGE_PER_PROC
        );
        if (!self.isAlive() && self instanceof SnowGolem && freezingOwner != null) {
            ScytheAdvancementTracker.tryGrantSupercooledSnow(freezingOwner);
        }
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
