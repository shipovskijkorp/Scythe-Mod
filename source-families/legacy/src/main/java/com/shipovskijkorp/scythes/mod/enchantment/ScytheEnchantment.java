package com.shipovskijkorp.scythes.mod.enchantment;

import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.item.BloodScytheItem;
import com.shipovskijkorp.scythes.mod.item.ToxicScytheItem;
import com.shipovskijkorp.scythes.mod.item.WitheringScytheItem;
import java.util.function.Predicate;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

/** Legacy adapter for the same tuning used by modern enchantment data. */
public final class ScytheEnchantment extends Enchantment {
    private final Predicate<ItemStack> accepts;
    private final int maxLevel;
    private final int minCost;
    private final int maxCost;
    private final int costPerLevel;

    private ScytheEnchantment(Predicate<ItemStack> accepts, int weight, int maxLevel,
                             int minCost, int maxCost, int costPerLevel) {
        super(rarityForWeight(weight), EnchantmentTarget.WEAPON, new EquipmentSlot[] {EquipmentSlot.MAINHAND});
        this.accepts = accepts;
        this.maxLevel = maxLevel;
        this.minCost = minCost;
        this.maxCost = maxCost;
        this.costPerLevel = costPerLevel;
    }

    private static Rarity rarityForWeight(int weight) {
        for (Rarity rarity : Rarity.values()) {
            if (rarity.getWeight() == weight) return rarity;
        }
        throw new IllegalArgumentException("Legacy enchantment weight must be 1, 2, 5 or 10: " + weight);
    }

    @Override public int getMaxLevel() { return maxLevel; }
    @Override public int getMinPower(int level) { return minCost + (level - 1) * costPerLevel; }
    @Override public int getMaxPower(int level) { return maxCost + (level - 1) * costPerLevel; }
    @Override public boolean isAcceptableItem(ItemStack stack) { return accepts.test(stack); }

    public static Enchantment spikedBlade() {
        return new ScytheEnchantment(stack -> stack.getItem() instanceof BloodScytheItem,
                ScytheBalance.Enchantments.SPIKED_BLADE_WEIGHT,
                ScytheBalance.Enchantments.SPIKED_BLADE_MAX_LEVEL,
                ScytheBalance.Enchantments.SPIKED_BLADE_MIN_COST,
                ScytheBalance.Enchantments.SPIKED_BLADE_MAX_COST,
                ScytheBalance.Enchantments.SPIKED_BLADE_COST_PER_LEVEL);
    }

    public static Enchantment additionalSlot() {
        return new ScytheEnchantment(stack -> stack.getItem() instanceof WitheringScytheItem,
                ScytheBalance.Enchantments.ADDITIONAL_SLOT_WEIGHT,
                ScytheBalance.Enchantments.ADDITIONAL_SLOT_MAX_LEVEL,
                ScytheBalance.Enchantments.ADDITIONAL_SLOT_MIN_COST,
                ScytheBalance.Enchantments.ADDITIONAL_SLOT_MAX_COST,
                ScytheBalance.Enchantments.ADDITIONAL_SLOT_COST_PER_LEVEL);
    }

    public static Enchantment soulSiphon() {
        return new ScytheEnchantment(stack -> stack.getItem() instanceof WitheringScytheItem,
                ScytheBalance.Enchantments.SOUL_SIPHON_WEIGHT,
                ScytheBalance.Enchantments.SOUL_SIPHON_MAX_LEVEL,
                ScytheBalance.Enchantments.SOUL_SIPHON_MIN_COST,
                ScytheBalance.Enchantments.SOUL_SIPHON_MAX_COST,
                ScytheBalance.Enchantments.SOUL_SIPHON_COST_PER_LEVEL);
    }

    public static Enchantment acidity() {
        return new ScytheEnchantment(stack -> stack.getItem() instanceof ToxicScytheItem,
                ScytheBalance.Enchantments.ACIDITY_WEIGHT,
                ScytheBalance.Enchantments.ACIDITY_MAX_LEVEL,
                ScytheBalance.Enchantments.ACIDITY_MIN_COST,
                ScytheBalance.Enchantments.ACIDITY_MAX_COST,
                ScytheBalance.Enchantments.ACIDITY_COST_PER_LEVEL);
    }

}
