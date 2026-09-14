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
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

public class ScytheModClient implements ClientModInitializer {

    private static KeyBinding scytheAbilityKey;

    public static Text getScytheAbilityKeyText(Formatting formatting) {
        if (scytheAbilityKey == null) {
            return Text.literal("R").formatted(formatting);
        }

        return scytheAbilityKey.getBoundKeyLocalizedText().copy().formatted(formatting);
    }

    @Override
    public void onInitializeClient() {

        EntityRendererRegistry.register(ScytheMod.TOXIC_ORB, FlyingItemEntityRenderer::new);
        EntityRendererRegistry.register(ScytheMod.ICE_SPIKE, IceSpikeEntityRenderer::new);
        EntityRendererRegistry.register(ScytheMod.WITHERING_MINION, WitherSkeletonEntityRenderer::new);

        // HUD
        BloodHarvestHudRenderer.register();
        ToxicAuraHudRenderer.register();
        WitheringAuraHudRenderer.register();

        // S2C: Blood Harvest HUD
        ClientPlayNetworking.registerGlobalReceiver(
                ModPackets.BloodHarvestStartPayload.ID,
                (payload, context) -> context.client().execute(() -> BloodHarvestHudState.startOrUpdate(payload.ticksLeft()))
        );
        ClientPlayNetworking.registerGlobalReceiver(
                ModPackets.BloodHarvestStopPayload.ID,
                (payload, context) -> context.client().execute(BloodHarvestHudState::stop)
        );

        // S2C: Toxic Aura HUD
        ClientPlayNetworking.registerGlobalReceiver(
                ModPackets.ToxicAuraStartPayload.ID,
                (payload, context) -> context.client().execute(() -> ToxicAuraHudState.startOrUpdate(payload.ticksLeft()))
        );
        ClientPlayNetworking.registerGlobalReceiver(
                ModPackets.ToxicAuraStopPayload.ID,
                (payload, context) -> context.client().execute(ToxicAuraHudState::stop)
        );

        // S2C: Withering Aura HUD
        ClientPlayNetworking.registerGlobalReceiver(
                ModPackets.WitheringAuraStartPayload.ID,
                (payload, context) -> context.client().execute(() -> WitheringAuraHudState.startOrUpdate(payload.ticksLeft()))
        );
        ClientPlayNetworking.registerGlobalReceiver(
                ModPackets.WitheringAuraStopPayload.ID,
                (payload, context) -> context.client().execute(WitheringAuraHudState::stop)
        );

        // Универсальная активная способность (R): Кровавая жатва, Токсичная аура и Иссушающая аура.
        scytheAbilityKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "key.scythes.scythe_ability",
                        InputUtil.Type.KEYSYM,
                        GLFW.GLFW_KEY_R,
                        KeyBinding.Category.create(ScytheMod.id("scythes"))
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
