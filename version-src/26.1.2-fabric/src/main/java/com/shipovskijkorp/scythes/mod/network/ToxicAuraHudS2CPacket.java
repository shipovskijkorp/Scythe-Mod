package com.shipovskijkorp.scythes.mod.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public final class ToxicAuraHudS2CPacket {
    private ToxicAuraHudS2CPacket() {}

    public static void sendTicks(ServerPlayer player, int ticksLeft) {
        if (!ServerPlayNetworking.canSend(player, ModPackets.ToxicAuraStartPayload.ID)) return;
        ServerPlayNetworking.send(player, new ModPackets.ToxicAuraStartPayload(Math.max(0, ticksLeft)));
    }

    public static void sendStop(ServerPlayer player) {
        if (!ServerPlayNetworking.canSend(player, ModPackets.ToxicAuraStopPayload.ID)) return;
        ServerPlayNetworking.send(player, new ModPackets.ToxicAuraStopPayload());
    }
}
