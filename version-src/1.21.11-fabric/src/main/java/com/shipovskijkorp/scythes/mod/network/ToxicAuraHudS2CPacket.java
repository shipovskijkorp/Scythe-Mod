package com.shipovskijkorp.scythes.mod.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

public final class ToxicAuraHudS2CPacket {

    private ToxicAuraHudS2CPacket() {}

    /** Start/Update (ticks left) */
    public static void sendTicks(ServerPlayerEntity player, int ticksLeft) {
        if (!ServerPlayNetworking.canSend(player, ModPackets.ToxicAuraStartPayload.ID)) return;
        ServerPlayNetworking.send(player, new ModPackets.ToxicAuraStartPayload(Math.max(0, ticksLeft)));
    }

    /** Stop */
    public static void sendStop(ServerPlayerEntity player) {
        if (!ServerPlayNetworking.canSend(player, ModPackets.ToxicAuraStopPayload.ID)) return;
        ServerPlayNetworking.send(player, new ModPackets.ToxicAuraStopPayload());
    }
}
