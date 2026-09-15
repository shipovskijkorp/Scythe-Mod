package com.shipovskijkorp.scythes.mod.network;

import com.shipovskijkorp.scythes.mod.ability.ScytheAbilityHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** Fabric transport; selection and gameplay live in ScytheAbilityHandler. */
public final class ScytheAbilityC2SPacket {
    private ScytheAbilityC2SPacket() {}

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(ModPackets.ScytheAbilityPayload.ID,
                (payload, context) -> {
                    var player = context.player();
                    var server = player.getServer();
                    if (server != null) server.execute(() -> ScytheAbilityHandler.activate(player));
                });
    }
}
