package com.shipovskijkorp.scythes.mod.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

public class BloodHarvestHudRenderer {

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        if (!BloodHarvestHudState.isActive() || !HeldScytheHudUtil.isHoldingBloodScythe()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        Text text = Text.translatable(
                "hud.scythes.blood_harvest",
                BloodHarvestHudState.getSecondsLeft()
        );

        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();

        int w = client.textRenderer.getWidth(text);
        int x = (sw - w) / 2;
        int y = sh / 2 + 30;

        context.drawText(client.textRenderer, text, x, y, 0xAA0000, true);
    }
}
