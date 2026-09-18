package com.shipovskijkorp.scythes.mod.network;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** NeoForge client transport; selection and gameplay live in ScytheAbilityHandler. */
public final class ScytheAbilityC2SPacket {
    private ScytheAbilityC2SPacket() {}

    public static void send() {
        ClientPacketDistributor.sendToServer(new ModPackets.ScytheAbilityPayload());
    }
}
