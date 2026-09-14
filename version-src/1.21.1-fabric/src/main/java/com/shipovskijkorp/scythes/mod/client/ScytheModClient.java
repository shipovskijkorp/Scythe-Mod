package com.shipovskijkorp.scythes.mod.client;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.network.ModPackets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.minecraft.client.render.entity.WitherSkeletonEntityRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class ScytheModClient implements ClientModInitializer {

    private static KeyBinding scytheAbilityKey;

    public static Text getScytheAbilityKeyBoundText() {
        return scytheAbilityKey == null
                ? Text.literal("R")
                : scytheAbilityKey.getBoundKeyLocalizedText();
    }

    @Override
    public void onInitializeClient() {

        EntityRendererRegistry.register(ScytheMod.TOXIC_ORB, FlyingItemEntityRenderer::new);
        EntityRendererRegistry.register(ScytheMod.ICE_SPIKE, IceSpikeEntityRenderer::new);
        EntityRendererRegistry.register(ScytheMod.WITHERING_MINION, WitherSkeletonEntityRenderer::new);

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

        scytheAbilityKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "key.scythes.scythe_ability",
                        InputUtil.Type.KEYSYM,
                        GLFW.GLFW_KEY_R,
                        "category.scythes"
                )
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            BloodHarvestHudState.tick();
            ToxicAuraHudState.tick();
            WitheringAuraHudState.tick();

            while (scytheAbilityKey.wasPressed()) {
                if (client.getNetworkHandler() != null && ClientPlayNetworking.canSend(ModPackets.ScytheAbilityPayload.ID)) {
                    ClientPlayNetworking.send(new ModPackets.ScytheAbilityPayload());
                }
            }
        });
    }
}
