package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public final class PlagueScytheMigrationHandler {

    private PlagueScytheMigrationHandler() {
    }

    public static void migratePlayer(ServerPlayer player) {
        int replaced = 0;
        replaced += migrateInventory(player.getInventory());
        replaced += migrateInventory(player.getEnderChestInventory());

        if (replaced > 0) {
            player.sendOverlayMessage(Component.translatable("message.scythes.plague_migrated", replaced));
        }
    }

    private static int migrateInventory(Container inventory) {
        int replaced = 0;

        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.is(ScytheMod.PLAGUE_SCYTHE)) continue;

            inventory.setItem(slot, createReplacement(stack));
            replaced++;
        }

        return replaced;
    }

    private static ItemStack createReplacement(ItemStack oldStack) {
        ItemStack replacement = oldStack.transmuteCopy(ScytheMod.TOXIC_SCYTHE, oldStack.getCount());

        if (oldStack.isDamageableItem() && replacement.isDamageableItem()) {
            replacement.setDamageValue(Math.min(oldStack.getDamageValue(), replacement.getMaxDamage() - 1));
        }

        return replacement;
    }
}
