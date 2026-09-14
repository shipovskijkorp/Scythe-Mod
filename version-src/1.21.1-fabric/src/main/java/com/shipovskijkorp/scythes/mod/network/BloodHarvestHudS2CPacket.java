package com.shipovskijkorp.scythes.mod.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

public final class BloodHarvestHudS2CPacket {

    private BloodHarvestHudS2CPacket() {}

    public static void sendTicks(ServerPlayerEntity player, int ticksLeft) {
        if (!ServerPlayNetworking.canSend(player, ModPackets.BloodHarvestStartPayload.ID)) return;
        ServerPlayNetworking.send(player, new ModPackets.BloodHarvestStartPayload(Math.max(0, ticksLeft)));
    }

    public static void sendStop(ServerPlayerEntity player) {
        if (!ServerPlayNetworking.canSend(player, ModPackets.BloodHarvestStopPayload.ID)) return;
        ServerPlayNetworking.send(player, new ModPackets.BloodHarvestStopPayload());
    }
}
