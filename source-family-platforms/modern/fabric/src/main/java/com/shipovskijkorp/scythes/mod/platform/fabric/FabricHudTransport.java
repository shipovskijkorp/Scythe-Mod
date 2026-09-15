package com.shipovskijkorp.scythes.mod.platform.fabric;

import com.shipovskijkorp.scythes.mod.network.ModPackets;
import com.shipovskijkorp.scythes.mod.platform.HudTransport;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

/** All Fabric S2C calls are isolated here, not in the ability trackers. */
public final class FabricHudTransport implements HudTransport<ServerPlayerEntity> {
    @Override
    public void start(ServerPlayerEntity player, Timer timer, int ticksLeft) {
        switch (timer) {
            case BLOOD_HARVEST -> {
                if (!ServerPlayNetworking.canSend(player, ModPackets.BloodHarvestStartPayload.ID)) return;
                ServerPlayNetworking.send(player, new ModPackets.BloodHarvestStartPayload(Math.max(0, ticksLeft)));
            }
            case TOXIC_AURA -> {
                if (!ServerPlayNetworking.canSend(player, ModPackets.ToxicAuraStartPayload.ID)) return;
                ServerPlayNetworking.send(player, new ModPackets.ToxicAuraStartPayload(Math.max(0, ticksLeft)));
            }
            case WITHERING_AURA -> {
                if (!ServerPlayNetworking.canSend(player, ModPackets.WitheringAuraStartPayload.ID)) return;
                ServerPlayNetworking.send(player, new ModPackets.WitheringAuraStartPayload(Math.max(0, ticksLeft)));
            }
        }
    }

    @Override
    public void stop(ServerPlayerEntity player, Timer timer) {
        switch (timer) {
            case BLOOD_HARVEST -> {
                if (!ServerPlayNetworking.canSend(player, ModPackets.BloodHarvestStopPayload.ID)) return;
                ServerPlayNetworking.send(player, new ModPackets.BloodHarvestStopPayload());
            }
            case TOXIC_AURA -> {
                if (!ServerPlayNetworking.canSend(player, ModPackets.ToxicAuraStopPayload.ID)) return;
                ServerPlayNetworking.send(player, new ModPackets.ToxicAuraStopPayload());
            }
            case WITHERING_AURA -> {
                if (!ServerPlayNetworking.canSend(player, ModPackets.WitheringAuraStopPayload.ID)) return;
                ServerPlayNetworking.send(player, new ModPackets.WitheringAuraStopPayload());
            }
        }
    }

}
