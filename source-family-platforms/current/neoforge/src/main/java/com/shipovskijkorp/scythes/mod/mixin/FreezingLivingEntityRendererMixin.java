package com.shipovskijkorp.scythes.mod.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.shipovskijkorp.scythes.mod.client.FreezingVisualClientState;
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

/**
 * Current-generation NeoForge uses explicit S2C freezing-state synchronization
 * instead of attaching foreign SynchedEntityData to vanilla LivingEntity.
 */
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
        ((FreezingRenderState) state).scythes$setFrozenForRendering(
                entity.isAlive() && FreezingVisualClientState.isFrozen(entity.getUUID())
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
