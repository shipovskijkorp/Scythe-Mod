package com.shipovskijkorp.scythes.mod.client;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class HeldScytheHudUtil {

    private HeldScytheHudUtil() {
    }

    public static boolean isHolding(Item item) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return false;
        }

        ItemStack mainHandStack = client.player.getMainHandItem();
        ItemStack offHandStack = client.player.getOffhandItem();
        return mainHandStack.is(item) || offHandStack.is(item);
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
