package com.shipovskijkorp.scythes.mod.client;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public final class HeldScytheHudUtil {

    private HeldScytheHudUtil() {
    }

    public static boolean isHolding(Item item) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return false;
        }

        ItemStack mainHandStack = client.player.getMainHandStack();
        ItemStack offHandStack = client.player.getOffHandStack();
        return mainHandStack.isOf(item) || offHandStack.isOf(item);
    }

    public static boolean isHoldingBloodScythe() {
        return isHolding(ScytheMod.BLOODY_SCYTHE);
    }

    public static boolean isHoldingToxicScythe() {
        return isHolding(ScytheMod.TOXIC_SCYTHE);
    }

    public static boolean isHoldingWitheringScythe() {
        return isHolding(ScytheMod.WITHERING_SCYTHE);
    }
}
