package com.shipovskijkorp.scythes.mod.platform.fabric;

import com.shipovskijkorp.scythes.mod.network.ModPackets;
import com.shipovskijkorp.scythes.mod.platform.HudTransport;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

/** All Fabric S2C calls are isolated here, not in the ability trackers. */
public final class FabricHudTransport implements HudTransport<ServerPlayerEntity> {
    @Override
    public void start(ServerPlayerEntity player, Timer timer, int ticksLeft) {
        switch (timer) {
            case BLOOD_HARVEST -> {
                if (!ServerPlayNetworking.canSend(player, ModPackets.BLOOD_HARVEST_START_S2C)) return;

                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeInt(Math.max(0, ticksLeft));
                ServerPlayNetworking.send(player, ModPackets.BLOOD_HARVEST_START_S2C, buf);
            }
            case TOXIC_AURA -> {
                if (!ServerPlayNetworking.canSend(player, ModPackets.TOXIC_AURA_START_S2C)) return;

                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeInt(Math.max(0, ticksLeft));
                ServerPlayNetworking.send(player, ModPackets.TOXIC_AURA_START_S2C, buf);
            }
            case WITHERING_AURA -> {
                if (!ServerPlayNetworking.canSend(player, ModPackets.WITHERING_AURA_START_S2C)) return;

                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeInt(Math.max(0, ticksLeft));
                ServerPlayNetworking.send(player, ModPackets.WITHERING_AURA_START_S2C, buf);
            }
        }
    }

    @Override
    public void stop(ServerPlayerEntity player, Timer timer) {
        switch (timer) {
            case BLOOD_HARVEST -> {
                if (!ServerPlayNetworking.canSend(player, ModPackets.BLOOD_HARVEST_STOP_S2C)) return;
                ServerPlayNetworking.send(player, ModPackets.BLOOD_HARVEST_STOP_S2C, PacketByteBufs.empty());
            }
            case TOXIC_AURA -> {
                if (!ServerPlayNetworking.canSend(player, ModPackets.TOXIC_AURA_STOP_S2C)) return;
                ServerPlayNetworking.send(player, ModPackets.TOXIC_AURA_STOP_S2C, PacketByteBufs.empty());
            }
            case WITHERING_AURA -> {
                if (!ServerPlayNetworking.canSend(player, ModPackets.WITHERING_AURA_STOP_S2C)) return;
                ServerPlayNetworking.send(player, ModPackets.WITHERING_AURA_STOP_S2C, PacketByteBufs.empty());
            }
        }
    }

}
