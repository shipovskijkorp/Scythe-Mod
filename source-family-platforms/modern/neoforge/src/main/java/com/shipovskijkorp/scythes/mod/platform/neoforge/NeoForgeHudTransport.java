package com.shipovskijkorp.scythes.mod.platform.neoforge;

import com.shipovskijkorp.scythes.mod.network.ModPackets;
import com.shipovskijkorp.scythes.mod.platform.HudTransport;
import net.minecraft.server.network.ServerPlayerEntity;
import net.neoforged.neoforge.network.PacketDistributor;

/** All NeoForge S2C calls are isolated here, not in the ability trackers. */
public final class NeoForgeHudTransport implements HudTransport<ServerPlayerEntity> {
    @Override
    public void start(ServerPlayerEntity player, Timer timer, int ticksLeft) {
        int safeTicks = Math.max(0, ticksLeft);
        switch (timer) {
            case BLOOD_HARVEST -> PacketDistributor.sendToPlayer(
                    player, new ModPackets.BloodHarvestStartPayload(safeTicks));
            case TOXIC_AURA -> PacketDistributor.sendToPlayer(
                    player, new ModPackets.ToxicAuraStartPayload(safeTicks));
            case WITHERING_AURA -> PacketDistributor.sendToPlayer(
                    player, new ModPackets.WitheringAuraStartPayload(safeTicks));
        }
    }

    @Override
    public void stop(ServerPlayerEntity player, Timer timer) {
        switch (timer) {
            case BLOOD_HARVEST -> PacketDistributor.sendToPlayer(
                    player, new ModPackets.BloodHarvestStopPayload());
            case TOXIC_AURA -> PacketDistributor.sendToPlayer(
                    player, new ModPackets.ToxicAuraStopPayload());
            case WITHERING_AURA -> PacketDistributor.sendToPlayer(
                    player, new ModPackets.WitheringAuraStopPayload());
        }
    }
}
