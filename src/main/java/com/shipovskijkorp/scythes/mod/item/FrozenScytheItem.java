package com.shipovskijkorp.scythes.mod.item;

import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;

/**
 * Base Frost Scythe implementation. It intentionally has no special abilities yet.
 */
public class FrozenScytheItem extends SwordItem {

    public FrozenScytheItem(Settings settings) {
        super(ToolMaterials.NETHERITE, 4, -2.8F, settings);
    }
}
