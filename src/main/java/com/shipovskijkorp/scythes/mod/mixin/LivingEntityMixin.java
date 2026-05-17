package com.shipovskijkorp.scythes.mod.mixin;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    private void scythes$noJump(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.hasStatusEffect(ScytheMod.NO_JUMP)) {
            ci.cancel();
        }
    }
}
