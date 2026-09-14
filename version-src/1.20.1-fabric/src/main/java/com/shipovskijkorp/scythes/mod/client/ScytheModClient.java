package com.shipovskijkorp.scythes.mod.client;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.network.ModPackets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
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

        EntityRendererRegistry.register(ScytheMod.ICE_SPIKE, IceSpikeEntityRenderer::new);
        EntityRendererRegistry.register(ScytheMod.TOXIC_ORB, FlyingItemEntityRenderer::new);
        EntityRendererRegistry.register(ScytheMod.WITHERING_MINION, WitherSkeletonEntityRenderer::new);

        // HUD
        BloodHarvestHudRenderer.register();
        ToxicAuraHudRenderer.register();
        WitheringAuraHudRenderer.register();

        // S2C: Blood Harvest HUD
        ClientPlayNetworking.registerGlobalReceiver(
                ModPackets.BLOOD_HARVEST_START_S2C,
                (client, handler, buf, responseSender) -> {
                    int ticks = buf.readInt();
                    client.execute(() -> BloodHarvestHudState.startOrUpdate(ticks));
                }
        );
        ClientPlayNetworking.registerGlobalReceiver(
                ModPackets.BLOOD_HARVEST_STOP_S2C,
                (client, handler, buf, responseSender) ->
                        client.execute(BloodHarvestHudState::stop)
        );

        // S2C: Toxic Aura HUD
        ClientPlayNetworking.registerGlobalReceiver(
                ModPackets.TOXIC_AURA_START_S2C,
                (client, handler, buf, responseSender) -> {
                    int ticks = buf.readInt();
                    client.execute(() -> ToxicAuraHudState.startOrUpdate(ticks));
                }
        );
        ClientPlayNetworking.registerGlobalReceiver(
                ModPackets.TOXIC_AURA_STOP_S2C,
                (client, handler, buf, responseSender) ->
                        client.execute(ToxicAuraHudState::stop)
        );

        // S2C: Withering Aura HUD
        ClientPlayNetworking.registerGlobalReceiver(
                ModPackets.WITHERING_AURA_START_S2C,
                (client, handler, buf, responseSender) -> {
                    int ticks = buf.readInt();
                    client.execute(() -> WitheringAuraHudState.startOrUpdate(ticks));
                }
        );
        ClientPlayNetworking.registerGlobalReceiver(
                ModPackets.WITHERING_AURA_STOP_S2C,
                (client, handler, buf, responseSender) ->
                        client.execute(WitheringAuraHudState::stop)
        );

        // ✅ Универсальная активная способность: Кровавая жатва, Токсичная аура и Иссушающая аура.
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
                if (client.getNetworkHandler() != null && ClientPlayNetworking.canSend(ModPackets.SCYTHE_ABILITY_C2S)) {
                    ClientPlayNetworking.send(ModPackets.SCYTHE_ABILITY_C2S, PacketByteBufs.empty());
                }
            }
        });
    }
}
