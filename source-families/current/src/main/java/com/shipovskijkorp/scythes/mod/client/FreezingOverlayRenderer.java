package com.shipovskijkorp.scythes.mod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.shipovskijkorp.scythes.mod.util.FreezingRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;

/** Draws a translucent vanilla-ice shell around every entity affected by Freezing. */
public final class FreezingOverlayRenderer {

    private static final SpriteId ICE_SPRITE = new SpriteId(
            TextureAtlas.LOCATION_BLOCKS,
            Identifier.withDefaultNamespace("block/ice")
    );
    private static final RenderType ICE_LAYER = RenderTypes.entityTranslucent(
            TextureAtlas.LOCATION_BLOCKS,
            false
    );
    private static final float SHELL_EXPANSION = 0.04F;
    private static final int ALPHA = 205;
    private static final int FULL_BRIGHT = 0x00F000F0;

    private FreezingOverlayRenderer() {
    }

    public static void render(
            LivingEntityRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        if (!(state instanceof FreezingRenderState freezingState)
                || !freezingState.scythes$isFrozenForRendering()) {
            return;
        }

        float halfWidth = Math.max(0.08F, state.boundingBoxWidth * 0.5F + SHELL_EXPANSION);
        float minY = -SHELL_EXPANSION;
        float maxY = Math.max(0.16F, state.boundingBoxHeight + SHELL_EXPANSION);
        TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager().get(ICE_SPRITE);

        collector.submitCustomGeometry(
                poseStack,
                ICE_LAYER,
                (pose, vertices) -> renderBox(pose, vertices, halfWidth, minY, maxY, sprite)
        );
    }

    private static void renderBox(
            PoseStack.Pose pose,
            VertexConsumer vertices,
            float halfWidth,
            float minY,
            float maxY,
            TextureAtlasSprite sprite
    ) {
        float minX = -halfWidth;
        float maxX = halfWidth;
        float minZ = -halfWidth;
        float maxZ = halfWidth;
        float minU = sprite.getU0();
        float maxU = sprite.getU1();
        float minV = sprite.getV0();
        float maxV = sprite.getV1();

        // North / south
        quad(pose, vertices, minX, minY, minZ, maxX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ,
                minU, minV, maxU, maxV, 0, 0, -1);
        quad(pose, vertices, maxX, minY, maxZ, minX, minY, maxZ, minX, maxY, maxZ, maxX, maxY, maxZ,
                minU, minV, maxU, maxV, 0, 0, 1);
        // West / east
        quad(pose, vertices, minX, minY, maxZ, minX, minY, minZ, minX, maxY, minZ, minX, maxY, maxZ,
                minU, minV, maxU, maxV, -1, 0, 0);
        quad(pose, vertices, maxX, minY, minZ, maxX, minY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ,
                minU, minV, maxU, maxV, 1, 0, 0);
        // Bottom / top
        quad(pose, vertices, minX, minY, maxZ, maxX, minY, maxZ, maxX, minY, minZ, minX, minY, minZ,
                minU, minV, maxU, maxV, 0, -1, 0);
        quad(pose, vertices, minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ,
                minU, minV, maxU, maxV, 0, 1, 0);
    }

    private static void quad(
            PoseStack.Pose pose,
            VertexConsumer vertices,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float x4, float y4, float z4,
            float minU, float minV, float maxU, float maxV,
            float normalX, float normalY, float normalZ
    ) {
        vertex(pose, vertices, x1, y1, z1, minU, maxV, normalX, normalY, normalZ);
        vertex(pose, vertices, x2, y2, z2, maxU, maxV, normalX, normalY, normalZ);
        vertex(pose, vertices, x3, y3, z3, maxU, minV, normalX, normalY, normalZ);
        vertex(pose, vertices, x4, y4, z4, minU, minV, normalX, normalY, normalZ);
    }

    private static void vertex(
            PoseStack.Pose pose,
            VertexConsumer vertices,
            float x,
            float y,
            float z,
            float u,
            float v,
            float normalX,
            float normalY,
            float normalZ
    ) {
        vertices.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, ALPHA)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(pose, normalX, normalY, normalZ);
    }
}
