package com.shipovskijkorp.scythes.mod.client;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.util.FreezingRenderState;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Draws a translucent ice shell around the complete hitbox of a frozen entity. */
public final class FreezingOverlayRenderer {

    private static final Identifier ICE_TEXTURE =
            new Identifier("minecraft", "textures/block/ice.png");
    private static final RenderLayer ICE_LAYER = RenderLayer.getEntityTranslucent(ICE_TEXTURE);

    private static final float HITBOX_EXPANSION = 0.04F;
    private static final int FULL_BRIGHT_LIGHT = 0x00F000F0;
    private static final int ALPHA = 205;

    private FreezingOverlayRenderer() {
    }

    /**
     * Renders after LivingEntityRenderer has finished the living model. Its
     * temporary pose transforms have already been popped, while the dispatcher
     * still keeps the matrix translated to the current entity.
     */
    public static void render(LivingEntity entity,
                              MatrixStack matrices,
                              VertexConsumerProvider vertexConsumers) {
        if (!entity.isAlive()) return;

        boolean frozenFromTracker = entity instanceof FreezingRenderState state
                && state.scythes$isFrozenForRendering();
        if (!frozenFromTracker && !entity.hasStatusEffect(ScytheMod.FREEZING)) return;

        float halfWidth = entity.getWidth() * 0.5F + HITBOX_EXPANSION;
        float minX = -halfWidth;
        float maxX = halfWidth;
        float minY = -HITBOX_EXPANSION;
        float maxY = entity.getHeight() + HITBOX_EXPANSION;
        float minZ = -halfWidth;
        float maxZ = halfWidth;

        MatrixStack.Entry entry = matrices.peek();
        Matrix4f positionMatrix = entry.getPositionMatrix();
        Matrix3f normalMatrix = entry.getNormalMatrix();
        VertexConsumer vertices = vertexConsumers.getBuffer(ICE_LAYER);

        // North (-Z)
        quad(vertices, positionMatrix, normalMatrix,
                minX, minY, minZ,
                minX, maxY, minZ,
                maxX, maxY, minZ,
                maxX, minY, minZ,
                0.0F, 0.0F, -1.0F);

        // South (+Z)
        quad(vertices, positionMatrix, normalMatrix,
                maxX, minY, maxZ,
                maxX, maxY, maxZ,
                minX, maxY, maxZ,
                minX, minY, maxZ,
                0.0F, 0.0F, 1.0F);

        // West (-X)
        quad(vertices, positionMatrix, normalMatrix,
                minX, minY, maxZ,
                minX, maxY, maxZ,
                minX, maxY, minZ,
                minX, minY, minZ,
                -1.0F, 0.0F, 0.0F);

        // East (+X)
        quad(vertices, positionMatrix, normalMatrix,
                maxX, minY, minZ,
                maxX, maxY, minZ,
                maxX, maxY, maxZ,
                maxX, minY, maxZ,
                1.0F, 0.0F, 0.0F);

        // Bottom (-Y)
        quad(vertices, positionMatrix, normalMatrix,
                minX, minY, maxZ,
                minX, minY, minZ,
                maxX, minY, minZ,
                maxX, minY, maxZ,
                0.0F, -1.0F, 0.0F);

        // Top (+Y)
        quad(vertices, positionMatrix, normalMatrix,
                minX, maxY, minZ,
                minX, maxY, maxZ,
                maxX, maxY, maxZ,
                maxX, maxY, minZ,
                0.0F, 1.0F, 0.0F);
    }

    private static void quad(VertexConsumer vertices,
                             Matrix4f positionMatrix,
                             Matrix3f normalMatrix,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             float x4, float y4, float z4,
                             float normalX, float normalY, float normalZ) {
        vertex(vertices, positionMatrix, normalMatrix,
                x1, y1, z1, 0.0F, 1.0F, normalX, normalY, normalZ);
        vertex(vertices, positionMatrix, normalMatrix,
                x2, y2, z2, 0.0F, 0.0F, normalX, normalY, normalZ);
        vertex(vertices, positionMatrix, normalMatrix,
                x3, y3, z3, 1.0F, 0.0F, normalX, normalY, normalZ);
        vertex(vertices, positionMatrix, normalMatrix,
                x4, y4, z4, 1.0F, 1.0F, normalX, normalY, normalZ);
    }

    private static void vertex(VertexConsumer vertices,
                               Matrix4f positionMatrix,
                               Matrix3f normalMatrix,
                               float x, float y, float z,
                               float u, float v,
                               float normalX, float normalY, float normalZ) {
        vertices.vertex(positionMatrix, x, y, z)
                .color(255, 255, 255, ALPHA)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(FULL_BRIGHT_LIGHT)
                .normal(normalMatrix, normalX, normalY, normalZ)
                .next();
    }
}
