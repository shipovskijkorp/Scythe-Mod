package com.shipovskijkorp.scythes.mod.enchantment;

import com.shipovskijkorp.scythes.mod.item.BloodScytheItem;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

public class SpikedBladeEnchantment extends Enchantment {

    public SpikedBladeEnchantment() {
        super(Rarity.RARE, EnchantmentTarget.WEAPON, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }

    /**
     * ✅ Жёсткое ограничение: чар подходит ТОЛЬКО для BloodScytheItem.
     * Это используется и столом зачарований, и наковальней, и командами (частично).
     */
    @Override
    public boolean isAcceptableItem(ItemStack stack) {
        return stack.getItem() instanceof BloodScytheItem;
    }

    /**
     * ✅ Доп. страховка: даже если кто-то попытается наложить чар не туда,
     * игра будет считать его несовместимым с предметом.
     */
    @Override
    public boolean canAccept(Enchantment other) {
        return super.canAccept(other);
    }
}
