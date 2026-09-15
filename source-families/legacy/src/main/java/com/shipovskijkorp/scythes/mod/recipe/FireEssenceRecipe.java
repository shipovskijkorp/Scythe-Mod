package com.shipovskijkorp.scythes.mod.recipe;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class FireEssenceRecipe extends SpecialCraftingRecipe {

    public FireEssenceRecipe(Identifier id, CraftingRecipeCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(RecipeInputInventory inventory, World world) {
        if (inventory.getWidth() != 3 || inventory.getHeight() != 3) {
            return false;
        }

        return inventory.getStack(0).isOf(Items.BLAZE_POWDER)
                && inventory.getStack(1).isOf(Items.FIRE_CHARGE)
                && inventory.getStack(2).isOf(Items.BLAZE_POWDER)
                && inventory.getStack(3).isOf(Items.LAVA_BUCKET)
                && inventory.getStack(4).isOf(Items.TNT)
                && inventory.getStack(5).isOf(Items.LAVA_BUCKET)
                && isPlainFlightOneFirework(inventory.getStack(6))
                && isPlainFlightOneFirework(inventory.getStack(7))
                && isPlainFlightOneFirework(inventory.getStack(8));
    }

    @Override
    public ItemStack craft(RecipeInputInventory inventory, DynamicRegistryManager registryManager) {
        return new ItemStack(ScytheMod.FIRE_ESSENCE, ScytheBalance.Crafting.FIRE_ESSENCE_OUTPUT_COUNT);
    }

    @Override
    public boolean fits(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public ItemStack getOutput(DynamicRegistryManager registryManager) {
        return new ItemStack(ScytheMod.FIRE_ESSENCE, ScytheBalance.Crafting.FIRE_ESSENCE_OUTPUT_COUNT);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ScytheMod.FIRE_ESSENCE_RECIPE_SERIALIZER;
    }

    private static boolean isPlainFlightOneFirework(ItemStack stack) {
        if (!stack.isOf(Items.FIREWORK_ROCKET)) {
            return false;
        }

        NbtCompound fireworks = stack.getSubNbt("Fireworks");
        if (fireworks == null || fireworks.getByte("Flight") != 1) {
            return false;
        }

        return fireworks.getList("Explosions", NbtElement.COMPOUND_TYPE).isEmpty();
    }
}
