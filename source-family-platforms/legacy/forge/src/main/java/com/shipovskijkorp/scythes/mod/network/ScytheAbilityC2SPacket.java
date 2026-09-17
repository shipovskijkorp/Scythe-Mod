package com.shipovskijkorp.scythes.mod.network;

import com.shipovskijkorp.scythes.mod.platform.forge.ForgeNetworking;

/** Forge transport; selection and gameplay live in ScytheAbilityHandler. */
public final class ScytheAbilityC2SPacket {
    private ScytheAbilityC2SPacket() {}

    public static void register() {
        ForgeNetworking.register();
    }

    public static void send() {
        ForgeNetworking.sendAbilityRequest();
    }
}
