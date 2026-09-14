package com.shipovskijkorp.scythes.mod.enchantment;

import com.shipovskijkorp.scythes.mod.item.ToxicScytheItem;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

public class AcidityEnchantment extends Enchantment {

    public AcidityEnchantment() {
        super(Rarity.UNCOMMON, EnchantmentTarget.WEAPON, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }

    @Override
    public int getMinPower(int level) {
        return 8 + (level - 1) * 12;
    }

    @Override
    public int getMaxPower(int level) {
        return getMinPower(level) + 35;
    }

    @Override
    public boolean isAcceptableItem(ItemStack stack) {
        return stack.getItem() instanceof ToxicScytheItem;
    }
}
