package com.shipovskijkorp.scythes.mod.platform.forge;

import com.shipovskijkorp.scythes.mod.platform.HudTransport;
import net.minecraft.server.network.ServerPlayerEntity;

/** All Forge S2C calls are isolated here, not in the ability trackers. */
public final class ForgeHudTransport implements HudTransport<ServerPlayerEntity> {
    @Override
    public void start(ServerPlayerEntity player, Timer timer, int ticksLeft) {
        ForgeNetworking.sendHud(player, timer, true, ticksLeft);
    }

    @Override
    public void stop(ServerPlayerEntity player, Timer timer) {
        ForgeNetworking.sendHud(player, timer, false, 0);
    }
}
