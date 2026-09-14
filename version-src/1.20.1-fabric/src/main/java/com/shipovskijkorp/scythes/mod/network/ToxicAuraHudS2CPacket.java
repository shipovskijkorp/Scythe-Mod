package com.shipovskijkorp.scythes.mod.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

public final class ToxicAuraHudS2CPacket {

    private ToxicAuraHudS2CPacket() {}

    /** Start/Update (ticks left) */
    public static void sendTicks(ServerPlayerEntity player, int ticksLeft) {
        if (!ServerPlayNetworking.canSend(player, ModPackets.TOXIC_AURA_START_S2C)) return;

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(Math.max(0, ticksLeft));
        ServerPlayNetworking.send(player, ModPackets.TOXIC_AURA_START_S2C, buf);
    }

    /** Stop */
    public static void sendStop(ServerPlayerEntity player) {
        if (!ServerPlayNetworking.canSend(player, ModPackets.TOXIC_AURA_STOP_S2C)) return;
        ServerPlayNetworking.send(player, ModPackets.TOXIC_AURA_STOP_S2C, PacketByteBufs.empty());
    }
}
