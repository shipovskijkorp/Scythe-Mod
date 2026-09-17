package com.shipovskijkorp.scythes.mod.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public final class ToxicAuraHudRenderer {
    private ToxicAuraHudRenderer() {}

    public static void render(DrawContext context) {
        if (!ToxicAuraHudState.isActive() || !HeldScytheHudUtil.isHoldingToxicScythe()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        Text text = Text.translatable("hud.scythes.toxic_aura", ToxicAuraHudState.getSecondsLeft());
        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();
        int x = (sw - client.textRenderer.getWidth(text)) / 2;
        int y = sh / 2 + 30;
        context.drawText(client.textRenderer, text, x, y, 0x2ECC40, true);
    }
}
