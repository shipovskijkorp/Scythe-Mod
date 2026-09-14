package com.shipovskijkorp.scythes.mod.mixin;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class FreezingEntityLookMixin {

    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void scythes$preventLookingWhileFrozen(double yRot, double xRot, CallbackInfo ci) {
        if ((Object) this instanceof LivingEntity living && living.hasEffect(ScytheMod.FREEZING)) {
            ci.cancel();
        }
    }
}
