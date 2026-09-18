package com.shipovskijkorp.scythes.mod.item;

import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.ToolMaterials;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.tag.TagKey;

/** MC adapter for shared balance; repair ingredients and mining tags stay vanilla. */
public enum ScytheMaterial implements ToolMaterial {
    INSTANCE,
    FARMER;

    @Override public int getDurability() { return ScytheBalance.Base.DURABILITY; }
    @Override public float getMiningSpeedMultiplier() { return ScytheBalance.Base.MINING_SPEED; }
    @Override public float getAttackDamage() {
        return this == FARMER ? ScytheBalance.Farmer.MATERIAL_ATTACK_DAMAGE : ScytheBalance.Base.MATERIAL_ATTACK_DAMAGE;
    }
    @Override public int getEnchantability() { return ScytheBalance.Base.ENCHANTABILITY; }
    @Override public TagKey<Block> getInverseTag() { return ToolMaterials.NETHERITE.getInverseTag(); }
    @Override public Ingredient getRepairIngredient() { return ToolMaterials.NETHERITE.getRepairIngredient(); }

    public static Item.Settings configureBase(Item.Settings settings) {
        settings.maxCount(ScytheBalance.Base.MAX_STACK_SIZE);
        if (ScytheBalance.Base.FIRE_RESISTANT) settings.fireproof();
        return settings;
    }

    public static Item.Settings configure(Item.Settings settings) {
        return configureBase(settings);
    }
}
