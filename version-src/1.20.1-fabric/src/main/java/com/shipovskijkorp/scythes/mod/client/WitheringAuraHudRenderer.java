package com.shipovskijkorp.scythes.mod.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class WitheringAuraHudRenderer {

    public static void register() {
        HudRenderCallback.EVENT.register(WitheringAuraHudRenderer::render);
    }

    private static void render(DrawContext context, float tickDelta) {
        if (!WitheringAuraHudState.isActive() || !HeldScytheHudUtil.isHoldingWitheringScythe()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        Text text = Text.translatable(
                "hud.scythes.withering_aura",
                WitheringAuraHudState.getSecondsLeft()
        );

        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();

        int w = client.textRenderer.getWidth(text);
        int x = (sw - w) / 2;
        int y = sh / 2 + 30;

        context.drawText(client.textRenderer, text, x, y, 0x8E44AD, true);
    }
}
