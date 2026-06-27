package com.shipovskijkorp.scythes.mod.item;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public final class ScytheItemUtil {

    private ScytheItemUtil() {
    }

    public static boolean isScythe(ItemStack stack) {
        return !stack.isEmpty() && isScythe(stack.getItem());
    }

    public static boolean isScythe(Item item) {
        return item instanceof BloodScytheItem
                || item instanceof ToxicScytheItem
                || item instanceof WitheringScytheItem
                || item instanceof GoldenScytheItem
                || item == ScytheMod.PLAGUE_SCYTHE;
    }

    public static boolean isBloodScythe(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof BloodScytheItem;
    }

    public static boolean isWitheringScythe(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof WitheringScytheItem;
    }

    public static boolean isToxicScythe(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ToxicScytheItem;
    }
}
