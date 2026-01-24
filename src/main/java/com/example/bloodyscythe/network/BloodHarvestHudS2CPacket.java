package com.example.bloodyscythe.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

public final class BloodHarvestHudS2CPacket {

    private BloodHarvestHudS2CPacket() {}

    /** Start/Update (ticks left) */
    public static void sendTicks(ServerPlayerEntity player, int ticksLeft) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(Math.max(0, ticksLeft));
        ServerPlayNetworking.send(player, ModPackets.BLOOD_HARVEST_START_S2C, buf);
    }

    /** Stop */
    public static void sendStop(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, ModPackets.BLOOD_HARVEST_STOP_S2C, PacketByteBufs.empty());
    }
}
