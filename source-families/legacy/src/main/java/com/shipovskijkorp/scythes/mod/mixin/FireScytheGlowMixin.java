package com.shipovskijkorp.scythes.mod.mixin;

import com.shipovskijkorp.scythes.mod.client.FireScytheLockHighlighter;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class FireScytheGlowMixin {
    @Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true)
    private void scythes$showFireScytheLock(CallbackInfoReturnable<Boolean> cir) {
        if (FireScytheLockHighlighter.isLocked((Entity) (Object) this)) cir.setReturnValue(true);
    }
}
