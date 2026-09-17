package com.shipovskijkorp.scythes.mod.client;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.network.ScytheAbilityC2SPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ScytheMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ForgeClientEvents {
    private ForgeClientEvents() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        MinecraftClient client = MinecraftClient.getInstance();
        BloodHarvestHudState.tick();
        ToxicAuraHudState.tick();
        WitheringAuraHudState.tick();
        FireScytheLockHighlighter.tick(client);

        KeyBinding abilityKey = ScytheModClient.getScytheAbilityKey();
        if (abilityKey == null) return;
        while (abilityKey.wasPressed()) {
            if (client.getNetworkHandler() != null) {
                ScytheAbilityC2SPacket.send();
            }
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        BloodHarvestHudRenderer.render(event.getGuiGraphics());
        ToxicAuraHudRenderer.render(event.getGuiGraphics());
        WitheringAuraHudRenderer.render(event.getGuiGraphics());
    }
}
