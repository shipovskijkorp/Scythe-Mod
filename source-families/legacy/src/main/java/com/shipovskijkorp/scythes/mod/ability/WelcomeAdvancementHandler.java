package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import net.minecraft.advancement.Advancement;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class WelcomeAdvancementHandler {

    private WelcomeAdvancementHandler() {}

    public static void grantRoot(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        Advancement adv = server.getAdvancementLoader()
                .get(new Identifier(ScytheMod.MOD_ID, "root"));

        if (adv != null) {
            player.getAdvancementTracker().grantCriterion(adv, "join");
        }
    }
}
