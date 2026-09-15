package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class WelcomeAdvancementHandler {

    private WelcomeAdvancementHandler() {}

    public static void grantRoot(ServerPlayer player) {
        MinecraftServer server = ((net.minecraft.server.level.ServerLevel) player.level()).getServer();
        if (server == null) return;

        AdvancementHolder guideGranted = server.getAdvancements().get(ScytheMod.id("guide_book_granted"));
        if (guideGranted != null && !player.getAdvancements().getOrStartProgress(guideGranted).isDone()) {
            giveGuideBook(player);
            player.getAdvancements().award(guideGranted, "granted");
        }

        AdvancementHolder root = server.getAdvancements().get(ScytheMod.id("root"));
        if (root != null) {
            player.getAdvancements().award(root, "join");
        }
    }

    private static void giveGuideBook(ServerPlayer player) {
        ItemStack guide = new ItemStack(ScytheMod.GUIDE_BOOK);
        player.getInventory().add(guide);
        if (!guide.isEmpty()) {
            player.drop(guide, false);
        }
    }
}
