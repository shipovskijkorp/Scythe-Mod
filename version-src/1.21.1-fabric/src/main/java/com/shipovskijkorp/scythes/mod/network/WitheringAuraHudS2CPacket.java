package com.shipovskijkorp.scythes.mod.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

public final class WitheringAuraHudS2CPacket {

    private WitheringAuraHudS2CPacket() {}

    public static void sendTicks(ServerPlayerEntity player, int ticksLeft) {
        if (!ServerPlayNetworking.canSend(player, ModPackets.WitheringAuraStartPayload.ID)) return;
        ServerPlayNetworking.send(player, new ModPackets.WitheringAuraStartPayload(Math.max(0, ticksLeft)));
    }

    public static void sendStop(ServerPlayerEntity player) {
        if (!ServerPlayNetworking.canSend(player, ModPackets.WitheringAuraStopPayload.ID)) return;
        ServerPlayNetworking.send(player, new ModPackets.WitheringAuraStopPayload());
    }
}
