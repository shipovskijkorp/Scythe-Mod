package com.shipovskijkorp.scythes.mod.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.client.guide.GuideScreen;
import com.shipovskijkorp.scythes.mod.item.FarmerScytheItem;
import com.shipovskijkorp.scythes.mod.item.GuideBookItem;
import com.shipovskijkorp.scythes.mod.item.ScytheSwordItem;
import com.shipovskijkorp.scythes.mod.network.ModPackets;
import com.shipovskijkorp.scythes.mod.network.ScytheAbilityC2SPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.WitherSkeletonRenderer;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = ScytheMod.MOD_ID, dist = Dist.CLIENT)
public final class ScytheModClient {
    private static final KeyMapping.Category SCYTHES_CATEGORY = new KeyMapping.Category(ScytheMod.id("scythes"));
    private static KeyMapping scytheAbilityKey;

    public ScytheModClient(IEventBus modBus) {
        GuideBookItem.installClientOpener(GuideScreen::open);
        ScytheSwordItem.installTooltipAppender(ScytheTooltips::append);
        FarmerScytheItem.installTooltipAppender(ScytheTooltips::append);

        modBus.addListener(this::registerKeyMappings);
        modBus.addListener(this::registerEntityRenderers);
        modBus.addListener(this::registerGuiLayers);
        modBus.addListener(this::registerClientPayloadHandlers);

        NeoForge.EVENT_BUS.addListener(ScytheModClient::onClientTick);
    }

    private void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.registerCategory(SCYTHES_CATEGORY);
        scytheAbilityKey = new KeyMapping(
                "key.scythes.scythe_ability",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_R,
                SCYTHES_CATEGORY
        );
        event.register(scytheAbilityKey);
    }

    private void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ScytheMod.TOXIC_ORB, ThrownItemRenderer::new);
        event.registerEntityRenderer(ScytheMod.FIREBALL, ThrownItemRenderer::new);
        event.registerEntityRenderer(ScytheMod.ICE_SPIKE, IceSpikeEntityRenderer::new);
        event.registerEntityRenderer(ScytheMod.WITHERING_MINION, WitherSkeletonRenderer::new);
    }

    private void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ScytheMod.id("blood_harvest_hud"), BloodHarvestHudRenderer::render);
        event.registerAboveAll(ScytheMod.id("toxic_aura_hud"), ToxicAuraHudRenderer::render);
        event.registerAboveAll(ScytheMod.id("withering_aura_hud"), WitheringAuraHudRenderer::render);
    }

    private void registerClientPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(ModPackets.BloodHarvestStartPayload.ID,
                (payload, context) -> BloodHarvestHudState.startOrUpdate(payload.ticksLeft()));
        event.register(ModPackets.BloodHarvestStopPayload.ID,
                (payload, context) -> BloodHarvestHudState.stop());
        event.register(ModPackets.ToxicAuraStartPayload.ID,
                (payload, context) -> ToxicAuraHudState.startOrUpdate(payload.ticksLeft()));
        event.register(ModPackets.ToxicAuraStopPayload.ID,
                (payload, context) -> ToxicAuraHudState.stop());
        event.register(ModPackets.WitheringAuraStartPayload.ID,
                (payload, context) -> WitheringAuraHudState.startOrUpdate(payload.ticksLeft()));
        event.register(ModPackets.WitheringAuraStopPayload.ID,
                (payload, context) -> WitheringAuraHudState.stop());
        event.register(ModPackets.FreezingVisualPayload.ID,
                (payload, context) -> FreezingVisualClientState.setFrozen(payload.entityUuid(), payload.frozen()));
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        BloodHarvestHudState.tick();
        ToxicAuraHudState.tick();
        WitheringAuraHudState.tick();
        FireScytheLockHighlighter.tick(client);
        FreezingVisualClientState.tick(client);

        if (scytheAbilityKey == null) return;
        while (scytheAbilityKey.consumeClick()) {
            if (client.getConnection() != null) {
                ScytheAbilityC2SPacket.send();
            }
        }
    }

    public static Component getScytheAbilityKeyText(ChatFormatting formatting) {
        if (scytheAbilityKey == null) {
            return Component.literal("R").withStyle(formatting);
        }
        return scytheAbilityKey.getTranslatedKeyMessage().copy().withStyle(formatting);
    }
}
