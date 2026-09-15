package com.shipovskijkorp.scythes.mod.recipe;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FireworksComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;

public class FireEssenceRecipe extends SpecialCraftingRecipe {

    public FireEssenceRecipe(CraftingRecipeCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingRecipeInput input, World world) {
        if (input.getWidth() != 3 || input.getHeight() != 3) {
            return false;
        }

        return isBlazePowder(get(input, 0))
                && get(input, 1).isOf(Items.FIRE_CHARGE)
                && isBlazePowder(get(input, 2))
                && get(input, 3).isOf(Items.LAVA_BUCKET)
                && get(input, 4).isOf(Items.TNT)
                && get(input, 5).isOf(Items.LAVA_BUCKET)
                && isPlainFlightOneFirework(get(input, 6))
                && isPlainFlightOneFirework(get(input, 7))
                && isPlainFlightOneFirework(get(input, 8));
    }

    @Override
    public ItemStack craft(CraftingRecipeInput input, RegistryWrapper.WrapperLookup registries) {
        return new ItemStack(ScytheMod.FIRE_ESSENCE, ScytheBalance.Crafting.FIRE_ESSENCE_OUTPUT_COUNT);
    }

    public boolean fits(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public RecipeSerializer<? extends SpecialCraftingRecipe> getSerializer() {
        return ScytheMod.FIRE_ESSENCE_RECIPE_SERIALIZER;
    }

    private static ItemStack get(CraftingRecipeInput input, int slot) {
        return input.getStackInSlot(slot % 3, slot / 3);
    }

    private static boolean isBlazePowder(ItemStack stack) {
        return stack.isOf(Items.BLAZE_POWDER);
    }

    private static boolean isPlainFlightOneFirework(ItemStack stack) {
        if (!stack.isOf(Items.FIREWORK_ROCKET)) {
            return false;
        }

        FireworksComponent fireworks = stack.get(DataComponentTypes.FIREWORKS);
        return fireworks != null && fireworks.flightDuration() == 1 && fireworks.explosions().isEmpty();
    }
}
