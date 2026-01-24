package com.example.bloodyscythe.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class BloodHarvestHudRenderer {

    public static void register() {
        HudRenderCallback.EVENT.register(BloodHarvestHudRenderer::render);
    }

    private static void render(DrawContext context, float tickDelta) {
        if (!BloodHarvestHudState.isActive()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        Text text = Text.translatable(
                "hud.bloodyscythe.blood_harvest",
                BloodHarvestHudState.getSecondsLeft()
        );

        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();

        int w = client.textRenderer.getWidth(text);
        int x = (sw - w) / 2;
        int y = sh / 2 + 30;

        context.drawText(
                client.textRenderer,
                text,
                x,
                y,
                0xAA0000,
                true
        );
    }
}
