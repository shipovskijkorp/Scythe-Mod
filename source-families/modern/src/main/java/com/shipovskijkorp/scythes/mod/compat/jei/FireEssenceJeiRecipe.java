package com.shipovskijkorp.scythes.mod.compat.jei;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import java.util.List;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FireworksComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/** Client-only JEI display model for the special Fire Essence crafting recipe. */
public final class FireEssenceJeiRecipe {

    private final ItemStack flightOneFirework;
    private final ItemStack output;

    public FireEssenceJeiRecipe() {
        this.flightOneFirework = createFlightOneFirework();
        this.output = new ItemStack(ScytheMod.FIRE_ESSENCE, ScytheBalance.Crafting.FIRE_ESSENCE_OUTPUT_COUNT);
    }

    private static ItemStack createFlightOneFirework() {
        ItemStack stack = new ItemStack(Items.FIREWORK_ROCKET);
        stack.set(DataComponentTypes.FIREWORKS, new FireworksComponent(1, List.of()));
        return stack;
    }

    public ItemStack getBlazePowder() { return new ItemStack(Items.BLAZE_POWDER); }
    public ItemStack getFireCharge() { return new ItemStack(Items.FIRE_CHARGE); }
    public ItemStack getLavaBucket() { return new ItemStack(Items.LAVA_BUCKET); }
    public ItemStack getTnt() { return new ItemStack(Items.TNT); }
    public ItemStack getFlightOneFirework() { return flightOneFirework.copy(); }
    public ItemStack getOutput() { return output.copy(); }
}
