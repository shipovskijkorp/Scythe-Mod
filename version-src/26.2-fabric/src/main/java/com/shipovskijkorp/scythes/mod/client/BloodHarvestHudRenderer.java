package com.shipovskijkorp.scythes.mod.client;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class BloodHarvestHudRenderer {
    public static void register() {
        HudElementRegistry.addLast(ScytheMod.id("blood_harvest_hud"), (context, tickCounter) -> {
            if (!BloodHarvestHudState.isActive() || !HeldScytheHudUtil.isHoldingBloodScythe()) return;
            Minecraft client = Minecraft.getInstance();
            if (client.player == null) return;
            Component text = Component.translatable("hud.scythes.blood_harvest", BloodHarvestHudState.getSecondsLeft());
            int sw = client.getWindow().getGuiScaledWidth();
            int sh = client.getWindow().getGuiScaledHeight();
            int w = client.font.width(text);
            context.text(client.font, text, (sw - w) / 2, sh / 2 + 30, 0xFFAA0000);
        });
    }
}
