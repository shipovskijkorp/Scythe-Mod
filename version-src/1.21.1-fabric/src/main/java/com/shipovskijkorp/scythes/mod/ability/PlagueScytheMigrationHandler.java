package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class PlagueScytheMigrationHandler {

    private PlagueScytheMigrationHandler() {
    }

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                server.execute(() -> migratePlayer(handler.player))
        );
    }

    public static void migratePlayer(ServerPlayerEntity player) {
        int replaced = 0;
        replaced += migrateInventory(player.getInventory());
        replaced += migrateInventory(player.getEnderChestInventory());

        if (replaced > 0) {
            player.sendMessage(Text.translatable("message.scythes.plague_migrated", replaced), false);
        }
    }

    private static int migrateInventory(Inventory inventory) {
        int replaced = 0;

        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.getStack(slot);
            if (!stack.isOf(ScytheMod.PLAGUE_SCYTHE)) continue;

            inventory.setStack(slot, createReplacement(stack));
            replaced++;
        }

        return replaced;
    }

    private static ItemStack createReplacement(ItemStack oldStack) {
        ItemStack replacement = new ItemStack(ScytheMod.TOXIC_SCYTHE, oldStack.getCount());

        replacement.applyChanges(oldStack.getComponentChanges());

        if (oldStack.isDamageable() && replacement.isDamageable()) {
            replacement.setDamage(Math.min(oldStack.getDamage(), replacement.getMaxDamage() - 1));
        }

        return replacement;
    }
}
