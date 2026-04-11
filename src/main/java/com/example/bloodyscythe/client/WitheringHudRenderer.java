package com.example.bloodyscythe.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class WitheringHudRenderer {

    public static void register() {
        HudRenderCallback.EVENT.register(WitheringHudRenderer::render);
    }

    private static void render(DrawContext context, float tickDelta) {
        if (!WitheringHudState.isActive() || !HeldScytheHudUtil.isHoldingWitheringScythe()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        Text text = Text.translatable("hud.bloodyscythe.withering", WitheringHudState.getSecondsLeft());

        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();

        int w = client.textRenderer.getWidth(text);
        int x = (sw - w) / 2;
        int y = sh / 2 + 30;

        context.drawText(client.textRenderer, text, x, y, 0x6B6B6B, true);
    }
}
