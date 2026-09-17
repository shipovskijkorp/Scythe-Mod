package com.shipovskijkorp.scythes.mod.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public final class WitheringAuraHudRenderer {
    private WitheringAuraHudRenderer() {}

    public static void render(DrawContext context) {
        if (!WitheringAuraHudState.isActive() || !HeldScytheHudUtil.isHoldingWitheringScythe()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        Text text = Text.translatable("hud.scythes.withering_aura", WitheringAuraHudState.getSecondsLeft());
        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();
        int x = (sw - client.textRenderer.getWidth(text)) / 2;
        int y = sh / 2 + 30;
        context.drawText(client.textRenderer, text, x, y, 0x8E44AD, true);
    }
}
