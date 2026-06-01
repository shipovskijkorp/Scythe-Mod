package com.shipovskijkorp.scythes.mod.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

public class WitheringHudS2CPacket {

    public static void sendTicks(ServerPlayerEntity player, int ticksLeft) {
        if (!ServerPlayNetworking.canSend(player, ModPackets.WITHERING_START_S2C)) return;

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(Math.max(0, ticksLeft));
        ServerPlayNetworking.send(player, ModPackets.WITHERING_START_S2C, buf);
    }

    public static void sendStop(ServerPlayerEntity player) {
        if (!ServerPlayNetworking.canSend(player, ModPackets.WITHERING_STOP_S2C)) return;
        ServerPlayNetworking.send(player, ModPackets.WITHERING_STOP_S2C, PacketByteBufs.empty());
    }
}
