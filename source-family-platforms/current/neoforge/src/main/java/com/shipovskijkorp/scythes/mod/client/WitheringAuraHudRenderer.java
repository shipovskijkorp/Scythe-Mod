package com.shipovskijkorp.scythes.mod.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class WitheringAuraHudRenderer {
    private WitheringAuraHudRenderer() {}

    public static void render(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
        if (!WitheringAuraHudState.isActive() || !HeldScytheHudUtil.isHoldingWitheringScythe()) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        Component text = Component.translatable("hud.scythes.withering_aura", WitheringAuraHudState.getSecondsLeft());
        int sw = client.getWindow().getGuiScaledWidth();
        int sh = client.getWindow().getGuiScaledHeight();
        int w = client.font.width(text);
        context.text(client.font, text, (sw - w) / 2, sh / 2 + 30, 0xFF8E44AD);
    }
}
