package com.shipovskijkorp.scythes.mod.item;

import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import net.minecraft.item.Item;
import net.minecraft.item.ToolMaterial;

/** MC adapter for shared balance; repair ingredients and mining tags stay vanilla. */
public final class ScytheMaterial {
    private ScytheMaterial() {}

    public static final ToolMaterial INSTANCE = new ToolMaterial(
            ToolMaterial.NETHERITE.incorrectBlocksForDrops(),
            ScytheBalance.Base.DURABILITY,
            ScytheBalance.Base.MINING_SPEED,
            ScytheBalance.Base.MATERIAL_ATTACK_DAMAGE,
            ScytheBalance.Base.ENCHANTABILITY,
            ToolMaterial.NETHERITE.repairItems());

    public static final ToolMaterial FARMER = new ToolMaterial(
            ToolMaterial.NETHERITE.incorrectBlocksForDrops(),
            ScytheBalance.Base.DURABILITY,
            ScytheBalance.Base.MINING_SPEED,
            ScytheBalance.Farmer.MATERIAL_ATTACK_DAMAGE,
            ScytheBalance.Base.ENCHANTABILITY,
            ToolMaterial.NETHERITE.repairItems());

    public static Item.Settings configureBase(Item.Settings settings) {
        settings.maxCount(ScytheBalance.Base.MAX_STACK_SIZE);
        if (ScytheBalance.Base.FIRE_RESISTANT) settings.fireproof();
        return settings;
    }

    public static Item.Settings configure(Item.Settings settings) {
        return configureBase(settings).sword(INSTANCE, ScytheBalance.Base.ATTACK_DAMAGE_BONUS, ScytheBalance.Base.ATTACK_SPEED_MODIFIER);
    }
}
