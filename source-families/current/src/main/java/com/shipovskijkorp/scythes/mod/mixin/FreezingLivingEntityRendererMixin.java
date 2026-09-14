package com.shipovskijkorp.scythes.mod.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.client.FreezingOverlayRenderer;
import com.shipovskijkorp.scythes.mod.util.FreezingRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class FreezingLivingEntityRendererMixin {

    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
            at = @At("TAIL")
    )
    private void scythes$copyFreezingState(
            LivingEntity entity,
            LivingEntityRenderState state,
            float partialTick,
            CallbackInfo ci
    ) {
        boolean trackedFrozen = entity instanceof FreezingRenderState freezingEntity
                && freezingEntity.scythes$isFrozenForRendering();

        ((FreezingRenderState) state).scythes$setFrozenForRendering(
                entity.isAlive() && (trackedFrozen || entity.hasEffect(ScytheMod.FREEZING))
        );
    }

    @Inject(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("TAIL")
    )
    private void scythes$renderFreezingOverlay(
            LivingEntityRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState cameraState,
            CallbackInfo ci
    ) {
        FreezingOverlayRenderer.render(state, poseStack, collector);
    }
}
