package com.shipovskijkorp.scythes.mod.mixin;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.client.FreezingOverlayRenderer;
import com.shipovskijkorp.scythes.mod.util.FreezingRenderState;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class FreezingLivingEntityRendererMixin {

    @Inject(
            method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V",
            at = @At("TAIL")
    )
    private void scythes$copyFreezingState(
            LivingEntity entity,
            LivingEntityRenderState state,
            float tickDelta,
            CallbackInfo ci
    ) {
        boolean frozenFromTracker = entity instanceof FreezingRenderState freezingEntity
                && freezingEntity.scythes$isFrozenForRendering();

        ((FreezingRenderState) state).scythes$setFrozenForRendering(
                entity.isAlive()
                        && (frozenFromTracker || entity.hasStatusEffect(ScytheMod.FREEZING))
        );
    }

    @Inject(
            method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
            at = @At("TAIL")
    )
    private void scythes$renderFreezingOverlay(
            LivingEntityRenderState state,
            MatrixStack matrices,
            OrderedRenderCommandQueue queue,
            CameraRenderState cameraState,
            CallbackInfo ci
    ) {
        FreezingOverlayRenderer.render(state, matrices, queue);
    }
}
