package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public final class WelcomeAdvancementHandler {

    private WelcomeAdvancementHandler() {}

    public static void grantRoot(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        AdvancementEntry guideGranted = server.getAdvancementLoader()
                .get(ScytheMod.id("guide_book_granted"));
        if (guideGranted != null && !player.getAdvancementTracker().getProgress(guideGranted).isDone()) {
            giveGuideBook(player);
            player.getAdvancementTracker().grantCriterion(guideGranted, "granted");
        }

        AdvancementEntry root = server.getAdvancementLoader()
                .get(ScytheMod.id("root"));
        if (root != null) {
            player.getAdvancementTracker().grantCriterion(root, "join");
        }
    }

    private static void giveGuideBook(ServerPlayerEntity player) {
        ItemStack guide = new ItemStack(ScytheMod.GUIDE_BOOK);
        player.getInventory().insertStack(guide);
        if (!guide.isEmpty()) {
            player.dropItem(guide, false);
        }
    }
}
