package com.shipovskijkorp.scythes.mod.client;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.util.FreezingRenderState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.entity.LivingEntity;

/** Draws a translucent ice shell around the complete hitbox of a frozen entity. */
public final class FreezingOverlayRenderer {

    /*
     * Block textures are stitched into the block atlas in 1.21.11. Sampling
     * textures/block/ice.png as a standalone entity texture can therefore resolve
     * incorrectly. Read the vanilla ice sprite from the atlas and use its actual
     * UV range instead.
     */
    private static final SpriteIdentifier ICE_SPRITE = new SpriteIdentifier(
            SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE,
            Identifier.ofVanilla("block/ice")
    );
    private static final RenderLayer ICE_LAYER = RenderLayers.entityTranslucent(
            SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE,
            false
    );

    private static final float HITBOX_EXPANSION = 0.04F;
    private static final int ALPHA = 205;
    private static final int FULL_BRIGHT_LIGHT = 0x00F000F0;

    private FreezingOverlayRenderer() {
    }

    /**
     * Resolves the client-visible freezing state without registering foreign
     * SynchedEntityData on LivingEntity. The status effect is the primary signal.
     * FROZEN_TICKS is a vanilla tracked value and is used as a narrow fallback
     * because the Freezing effect pins it immediately below the vanilla damage
     * threshold on the server.
     */
    public static boolean shouldRenderFor(LivingEntity entity) {
        if (!entity.isAlive()) {
            return false;
        }
        if (entity.hasStatusEffect(ScytheMod.FREEZING)) {
            return true;
        }

        int threshold = entity.getMinFreezeDamageTicks();
        if (threshold <= 0) {
            return false;
        }

        int frozenTicks = entity.getFrozenTicks();
        int fallbackFloor = Math.max(1, threshold - 4);
        return frozenTicks >= fallbackFloor && frozenTicks < threshold;
    }

    public static void render(
            LivingEntityRenderState state,
            MatrixStack matrices,
            OrderedRenderCommandQueue queue
    ) {
        if (!(state instanceof FreezingRenderState freezingState)
                || !freezingState.scythes$isFrozenForRendering()) {
            return;
        }

        Sprite sprite = MinecraftClient.getInstance().getAtlasManager().getSprite(ICE_SPRITE);
        float minU = sprite.getMinU();
        float maxU = sprite.getMaxU();
        float minV = sprite.getMinV();
        float maxV = sprite.getMaxV();

        float halfWidth = state.width * 0.5F + HITBOX_EXPANSION;
        float minX = -halfWidth;
        float maxX = halfWidth;
        float minY = -HITBOX_EXPANSION;
        float maxY = state.height + HITBOX_EXPANSION;
        float minZ = -halfWidth;
        float maxZ = halfWidth;

        queue.submitCustom(matrices, ICE_LAYER, (entry, vertices) -> {
            quad(vertices, entry,
                    minX, minY, minZ,
                    minX, maxY, minZ,
                    maxX, maxY, minZ,
                    maxX, minY, minZ,
                    minU, maxU, minV, maxV,
                    0.0F, 0.0F, -1.0F);

            quad(vertices, entry,
                    maxX, minY, maxZ,
                    maxX, maxY, maxZ,
                    minX, maxY, maxZ,
                    minX, minY, maxZ,
                    minU, maxU, minV, maxV,
                    0.0F, 0.0F, 1.0F);

            quad(vertices, entry,
                    minX, minY, maxZ,
                    minX, maxY, maxZ,
                    minX, maxY, minZ,
                    minX, minY, minZ,
                    minU, maxU, minV, maxV,
                    -1.0F, 0.0F, 0.0F);

            quad(vertices, entry,
                    maxX, minY, minZ,
                    maxX, maxY, minZ,
                    maxX, maxY, maxZ,
                    maxX, minY, maxZ,
                    minU, maxU, minV, maxV,
                    1.0F, 0.0F, 0.0F);

            quad(vertices, entry,
                    minX, minY, maxZ,
                    minX, minY, minZ,
                    maxX, minY, minZ,
                    maxX, minY, maxZ,
                    minU, maxU, minV, maxV,
                    0.0F, -1.0F, 0.0F);

            quad(vertices, entry,
                    minX, maxY, minZ,
                    minX, maxY, maxZ,
                    maxX, maxY, maxZ,
                    maxX, maxY, minZ,
                    minU, maxU, minV, maxV,
                    0.0F, 1.0F, 0.0F);
        });
    }

    private static void quad(
            VertexConsumer vertices,
            MatrixStack.Entry entry,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float x4, float y4, float z4,
            float minU, float maxU, float minV, float maxV,
            float normalX, float normalY, float normalZ
    ) {
        vertex(vertices, entry, x1, y1, z1, minU, maxV, normalX, normalY, normalZ);
        vertex(vertices, entry, x2, y2, z2, minU, minV, normalX, normalY, normalZ);
        vertex(vertices, entry, x3, y3, z3, maxU, minV, normalX, normalY, normalZ);
        vertex(vertices, entry, x4, y4, z4, maxU, maxV, normalX, normalY, normalZ);
    }

    private static void vertex(
            VertexConsumer vertices,
            MatrixStack.Entry entry,
            float x, float y, float z,
            float u, float v,
            float normalX, float normalY, float normalZ
    ) {
        vertices.vertex(entry, x, y, z)
                .color(255, 255, 255, ALPHA)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(FULL_BRIGHT_LIGHT)
                .normal(entry, normalX, normalY, normalZ);
    }
}
