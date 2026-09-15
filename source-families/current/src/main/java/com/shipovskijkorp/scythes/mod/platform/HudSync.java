package com.shipovskijkorp.scythes.mod.platform;

import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;

/** Installed by the loader entry point before any game event can fire. */
public final class HudSync {
    private static HudTransport<ServerPlayer> transport;
    private HudSync() {}

    public static void install(HudTransport<ServerPlayer> implementation) {
        if (transport != null) throw new IllegalStateException("HUD transport already installed");
        transport = Objects.requireNonNull(implementation, "implementation");
    }

    private static HudTransport<ServerPlayer> transport() {
        if (transport == null) throw new IllegalStateException("Loader did not install the HUD transport");
        return transport;
    }

    public static void start(ServerPlayer player, HudTransport.Timer timer, int ticksLeft) {
        transport().start(player, timer, Math.max(0, ticksLeft));
    }

    public static void stop(ServerPlayer player, HudTransport.Timer timer) {
        transport().stop(player, timer);
    }
}
