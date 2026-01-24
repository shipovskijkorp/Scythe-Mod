package com.example.bloodyscythe.ability;

import com.example.bloodyscythe.BleedingMod;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.advancement.Advancement;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

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

        Advancement adv = server.getAdvancementLoader()
                .get(new Identifier(BleedingMod.MOD_ID, "root"));

        if (adv != null) {
            player.getAdvancementTracker().grantCriterion(adv, "join");
        }
    }
}
