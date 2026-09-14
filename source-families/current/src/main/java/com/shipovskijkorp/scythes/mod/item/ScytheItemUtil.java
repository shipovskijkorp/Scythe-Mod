package com.shipovskijkorp.scythes.mod.item;

import net.minecraft.world.item.ItemStack;

public final class ScytheItemUtil {

    private ScytheItemUtil() {
    }

    public static boolean isScythe(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ScytheSwordItem;
    }

    public static boolean isBloodScythe(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof BloodScytheItem;
    }

    public static boolean isToxicScythe(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ToxicScytheItem;
    }

    public static boolean isWitheringScythe(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof WitheringScytheItem;
    }

    public static boolean isGoldenScythe(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof GoldenScytheItem;
    }
}
