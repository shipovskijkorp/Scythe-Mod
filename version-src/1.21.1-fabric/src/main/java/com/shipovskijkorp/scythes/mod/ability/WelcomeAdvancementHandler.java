package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public final class WelcomeAdvancementHandler {

    private WelcomeAdvancementHandler() {}

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;
            server.execute(() -> grantRoot(player));
        });
    }

    private static void grantRoot(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        AdvancementEntry adv = server.getAdvancementLoader()
                .get(ScytheMod.id("root"));

        if (adv != null) {
            player.getAdvancementTracker().grantCriterion(adv, "join");
        }
    }
}
