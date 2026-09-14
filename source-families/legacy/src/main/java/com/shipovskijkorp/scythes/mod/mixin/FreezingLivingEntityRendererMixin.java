package com.shipovskijkorp.scythes.mod.mixin;

import com.shipovskijkorp.scythes.mod.client.FreezingOverlayRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Renders the ice shell after the living model so the shell cannot depth-mask it. */
@Mixin(LivingEntityRenderer.class)
public abstract class FreezingLivingEntityRendererMixin {

    @Inject(
            method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("TAIL")
    )
    private void scythes$renderFreezingOverlay(LivingEntity entity,
                                                float yaw,
                                                float tickDelta,
                                                MatrixStack matrices,
                                                VertexConsumerProvider vertexConsumers,
                                                int light,
                                                CallbackInfo ci) {
        FreezingOverlayRenderer.render(entity, matrices, vertexConsumers);
    }
}
