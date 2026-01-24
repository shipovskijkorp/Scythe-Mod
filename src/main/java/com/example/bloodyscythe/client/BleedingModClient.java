package com.example.bloodyscythe.client;

import com.example.bloodyscythe.network.ModPackets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class BleedingModClient implements ClientModInitializer {

    private static KeyBinding scytheAbilityKey;

    @Override
    public void onInitializeClient() {

        // HUD
        BloodHarvestHudRenderer.register();
        PlagueHudRenderer.register();
        WitheringHudRenderer.register();

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

        // S2C: Plague HUD
        ClientPlayNetworking.registerGlobalReceiver(
                ModPackets.PLAGUE_START_S2C,
                (client, handler, buf, responseSender) -> {
                    int ticks = buf.readInt();
                    client.execute(() -> PlagueHudState.startOrUpdate(ticks));
                }
        );
        ClientPlayNetworking.registerGlobalReceiver(
                ModPackets.PLAGUE_STOP_S2C,
                (client, handler, buf, responseSender) ->
                        client.execute(PlagueHudState::stop)
        );

        // S2C: Withering HUD
        ClientPlayNetworking.registerGlobalReceiver(
                ModPackets.WITHERING_START_S2C,
                (client, handler, buf, responseSender) -> {
                    int ticks = buf.readInt();
                    client.execute(() -> WitheringHudState.startOrUpdate(ticks));
                }
        );
        ClientPlayNetworking.registerGlobalReceiver(
                ModPackets.WITHERING_STOP_S2C,
                (client, handler, buf, responseSender) ->
                        client.execute(WitheringHudState::stop)
        );

        // ✅ Универсальная активка (R)
        scytheAbilityKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "key.bloodyscythe.scythe_ability",
                        InputUtil.Type.KEYSYM,
                        GLFW.GLFW_KEY_R,
                        "category.bloodyscythe"
                )
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            BloodHarvestHudState.tick();
            PlagueHudState.tick();
            WitheringHudState.tick();

            while (scytheAbilityKey.wasPressed()) {
                ClientPlayNetworking.send(ModPackets.SCYTHE_ABILITY_C2S, PacketByteBufs.empty());
            }
        });
    }
}
