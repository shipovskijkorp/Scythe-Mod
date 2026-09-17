package com.shipovskijkorp.scythes.mod.platform.forge;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.ability.ScytheAbilityHandler;
import com.shipovskijkorp.scythes.mod.client.ForgeClientPacketHandler;
import com.shipovskijkorp.scythes.mod.platform.HudTransport;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public final class ForgeNetworking {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new Identifier(ScytheMod.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );
    private static boolean registered;

    private ForgeNetworking() {}

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        int id = 0;
        CHANNEL.registerMessage(
                id++,
                AbilityRequest.class,
                ForgeNetworking::encodeAbilityRequest,
                ForgeNetworking::decodeAbilityRequest,
                (message, contextSupplier) -> {
                    var context = contextSupplier.get();
                    ServerPlayerEntity player = context.getSender();
                    if (player != null) {
                        context.enqueueWork(() -> ScytheAbilityHandler.activate(player));
                    }
                    context.setPacketHandled(true);
                },
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                id,
                HudTimerMessage.class,
                ForgeNetworking::encodeHudTimer,
                ForgeNetworking::decodeHudTimer,
                (message, contextSupplier) -> {
                    var context = contextSupplier.get();
                    context.enqueueWork(() -> ForgeClientPacketHandler.handleHud(
                            message.timerId(), message.active(), message.ticksLeft()
                    ));
                    context.setPacketHandled(true);
                },
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
    }

    public static void sendAbilityRequest() {
        CHANNEL.sendToServer(AbilityRequest.INSTANCE);
    }

    public static void sendHud(ServerPlayerEntity player, HudTransport.Timer timer, boolean active, int ticksLeft) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new HudTimerMessage(timer.ordinal(), active, Math.max(0, ticksLeft))
        );
    }

    private static void encodeAbilityRequest(AbilityRequest message, PacketByteBuf buffer) {
    }

    private static AbilityRequest decodeAbilityRequest(PacketByteBuf buffer) {
        return AbilityRequest.INSTANCE;
    }

    private static void encodeHudTimer(HudTimerMessage message, PacketByteBuf buffer) {
        buffer.writeVarInt(message.timerId());
        buffer.writeBoolean(message.active());
        buffer.writeVarInt(message.ticksLeft());
    }

    private static HudTimerMessage decodeHudTimer(PacketByteBuf buffer) {
        return new HudTimerMessage(buffer.readVarInt(), buffer.readBoolean(), buffer.readVarInt());
    }

    private enum AbilityRequest {
        INSTANCE
    }

    private record HudTimerMessage(int timerId, boolean active, int ticksLeft) {
    }
}
