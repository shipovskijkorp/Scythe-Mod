package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import net.minecraft.advancement.Advancement;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class WelcomeAdvancementHandler {

    private static final Identifier ROOT_ID = new Identifier(ScytheMod.MOD_ID, "root");
    private static final Identifier GUIDE_GRANTED_ID = new Identifier(ScytheMod.MOD_ID, "guide_book_granted");

    private WelcomeAdvancementHandler() {}

    public static void grantRoot(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        Advancement guideGranted = server.getAdvancementLoader().get(GUIDE_GRANTED_ID);
        if (guideGranted != null && !player.getAdvancementTracker().getProgress(guideGranted).isDone()) {
            giveGuideBook(player);
            player.getAdvancementTracker().grantCriterion(guideGranted, "granted");
        }

        Advancement root = server.getAdvancementLoader().get(ROOT_ID);
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
