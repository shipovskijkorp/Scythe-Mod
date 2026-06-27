package com.shipovskijkorp.scythes.mod.enchantment;

import com.shipovskijkorp.scythes.mod.item.WitheringScytheItem;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

public class AdditionalSlotEnchantment extends Enchantment {

    public AdditionalSlotEnchantment() {
        super(Rarity.RARE, EnchantmentTarget.WEAPON, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }

    @Override
    public int getMinPower(int level) {
        return 12 + (level - 1) * 15;
    }

    @Override
    public int getMaxPower(int level) {
        return getMinPower(level) + 35;
    }

    @Override
    public boolean isAcceptableItem(ItemStack stack) {
        return stack.getItem() instanceof WitheringScytheItem;
    }
}
