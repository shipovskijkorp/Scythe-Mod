package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class WelcomeAdvancementHandler {

    private WelcomeAdvancementHandler() {}

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            server.execute(() -> grantRoot(player));
        });
    }

    private static void grantRoot(ServerPlayer player) {
        MinecraftServer server = ((net.minecraft.server.level.ServerLevel) player.level()).getServer();
        if (server == null) return;

        AdvancementHolder advancement = server.getAdvancements().get(ScytheMod.id("root"));
        if (advancement != null) {
            player.getAdvancements().award(advancement, "join");
        }
    }
}
