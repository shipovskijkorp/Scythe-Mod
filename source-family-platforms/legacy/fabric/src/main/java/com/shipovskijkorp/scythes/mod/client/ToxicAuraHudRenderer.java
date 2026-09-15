package com.shipovskijkorp.scythes.mod.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class ToxicAuraHudRenderer {

    public static void register() {
        HudRenderCallback.EVENT.register(ToxicAuraHudRenderer::render);
    }

    private static void render(DrawContext context, float tickDelta) {
        if (!ToxicAuraHudState.isActive() || !HeldScytheHudUtil.isHoldingToxicScythe()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        Text text = Text.translatable(
                "hud.scythes.toxic_aura",
                ToxicAuraHudState.getSecondsLeft()
        );

        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();

        int w = client.textRenderer.getWidth(text);
        int x = (sw - w) / 2;
        int y = sh / 2 + 30;

        context.drawText(client.textRenderer, text, x, y, 0x2ECC40, true);
    }
}
