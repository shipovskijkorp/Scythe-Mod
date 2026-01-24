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
        if (!WitheringHudState.isActive()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        Text text = Text.translatable("hud.bloodyscythe.withering", WitheringHudState.getSecondsLeft());

        int x = client.getWindow().getScaledWidth() / 2 - 60;
        int y = client.getWindow().getScaledHeight() / 2 + 45;

        context.drawText(
                client.textRenderer,
                text,
                x,
                y,
                0x6B6B6B,
                true
        );
    }
}
