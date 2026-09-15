package com.shipovskijkorp.scythes.mod.client;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class ToxicAuraHudRenderer {
    public static void register() {
        HudElementRegistry.addLast(ScytheMod.id("toxic_aura_hud"), (context, tickCounter) -> {
            if (!ToxicAuraHudState.isActive() || !HeldScytheHudUtil.isHoldingToxicScythe()) return;
            Minecraft client = Minecraft.getInstance();
            if (client.player == null) return;
            Component text = Component.translatable("hud.scythes.toxic_aura", ToxicAuraHudState.getSecondsLeft());
            int sw = client.getWindow().getGuiScaledWidth();
            int sh = client.getWindow().getGuiScaledHeight();
            int w = client.font.width(text);
            context.text(client.font, text, (sw - w) / 2, sh / 2 + 30, 0xFF2ECC40);
        });
    }
}
