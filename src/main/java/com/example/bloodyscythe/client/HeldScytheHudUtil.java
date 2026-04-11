package com.example.bloodyscythe.client;

import com.example.bloodyscythe.BleedingMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public final class HeldScytheHudUtil {

    private HeldScytheHudUtil() {
    }

    public static boolean isHoldingMainHand(Item item) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return false;
        }

        ItemStack mainHandStack = client.player.getMainHandStack();
        return mainHandStack.isOf(item);
    }

    public static boolean isHoldingBloodyScythe() {
        return isHoldingMainHand(BleedingMod.BLOODY_SCYTHE);
    }

    public static boolean isHoldingPlagueScythe() {
        return isHoldingMainHand(BleedingMod.PLAGUE_SCYTHE);
    }

    public static boolean isHoldingWitheringScythe() {
        return isHoldingMainHand(BleedingMod.WITHERING_SCYTHE);
    }
}
