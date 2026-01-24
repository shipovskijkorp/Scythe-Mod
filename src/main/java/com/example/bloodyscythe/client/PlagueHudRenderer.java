package com.example.bloodyscythe.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class PlagueHudRenderer {

    public static void register() {
        HudRenderCallback.EVENT.register(PlagueHudRenderer::render);
    }

    private static void render(DrawContext context, float tickDelta) {
        if (!PlagueHudState.isActive()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        Text text = Text.translatable(
                "hud.bloodyscythe.plague",
                PlagueHudState.getSecondsLeft()
        );

        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();

        int w = client.textRenderer.getWidth(text);
        int x = (sw - w) / 2;
        int y = sh / 2 + 42;

        context.drawText(
                client.textRenderer,
                text,
                x,
                y,
                0x3AA13A,
                true
        );
    }
}
