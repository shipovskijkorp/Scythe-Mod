package com.shipovskijkorp.scythes.mod.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.item.ScytheSwordItem;
import com.shipovskijkorp.scythes.mod.item.FarmerScytheItem;
import com.shipovskijkorp.scythes.mod.network.ModPackets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.WitherSkeletonRenderer;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class ScytheModClient implements ClientModInitializer {

    private static final KeyMapping.Category SCYTHES_CATEGORY = KeyMapping.Category.register(ScytheMod.id("scythes"));
    private static KeyMapping scytheAbilityKey;

    public static Component getScytheAbilityKeyText(ChatFormatting formatting) {
        if (scytheAbilityKey == null) {
            return Component.literal("R").withStyle(formatting);
        }
        return scytheAbilityKey.getTranslatedKeyMessage().copy().withStyle(formatting);
    }

    @Override
    public void onInitializeClient() {
        ScytheSwordItem.installTooltipAppender(ScytheTooltips::append);
        FarmerScytheItem.installTooltipAppender(ScytheTooltips::append);
        EntityRendererRegistry.register(ScytheMod.TOXIC_ORB, ThrownItemRenderer::new);
        EntityRendererRegistry.register(ScytheMod.ICE_SPIKE, IceSpikeEntityRenderer::new);
        EntityRendererRegistry.register(ScytheMod.WITHERING_MINION, WitherSkeletonRenderer::new);

        BloodHarvestHudRenderer.register();
        ToxicAuraHudRenderer.register();
        WitheringAuraHudRenderer.register();

        ClientPlayNetworking.registerGlobalReceiver(
                ModPackets.BloodHarvestStartPayload.ID,
                (payload, context) -> context.client().execute(() -> BloodHarvestHudState.startOrUpdate(payload.ticksLeft()))
        );
        ClientPlayNetworking.registerGlobalReceiver(
                ModPackets.BloodHarvestStopPayload.ID,
                (payload, context) -> context.client().execute(BloodHarvestHudState::stop)
        );

        ClientPlayNetworking.registerGlobalReceiver(
                ModPackets.ToxicAuraStartPayload.ID,
                (payload, context) -> context.client().execute(() -> ToxicAuraHudState.startOrUpdate(payload.ticksLeft()))
        );
        ClientPlayNetworking.registerGlobalReceiver(
                ModPackets.ToxicAuraStopPayload.ID,
                (payload, context) -> context.client().execute(ToxicAuraHudState::stop)
        );

        ClientPlayNetworking.registerGlobalReceiver(
                ModPackets.WitheringAuraStartPayload.ID,
                (payload, context) -> context.client().execute(() -> WitheringAuraHudState.startOrUpdate(payload.ticksLeft()))
        );
        ClientPlayNetworking.registerGlobalReceiver(
                ModPackets.WitheringAuraStopPayload.ID,
                (payload, context) -> context.client().execute(WitheringAuraHudState::stop)
        );

        scytheAbilityKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping(
                        "key.scythes.scythe_ability",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_R,
                        SCYTHES_CATEGORY
                )
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            BloodHarvestHudState.tick();
            ToxicAuraHudState.tick();
            WitheringAuraHudState.tick();

            while (scytheAbilityKey.consumeClick()) {
                if (client.getConnection() != null && ClientPlayNetworking.canSend(ModPackets.ScytheAbilityPayload.ID)) {
                    ClientPlayNetworking.send(new ModPackets.ScytheAbilityPayload());
                }
            }
        });
    }
}
