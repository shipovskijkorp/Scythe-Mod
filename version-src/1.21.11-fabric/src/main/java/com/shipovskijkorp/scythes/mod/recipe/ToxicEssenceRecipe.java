package com.shipovskijkorp.scythes.mod.recipe;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potions;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;

public class ToxicEssenceRecipe extends SpecialCraftingRecipe {

    public ToxicEssenceRecipe(CraftingRecipeCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingRecipeInput input, World world) {
        if (input.getWidth() != 3 || input.getHeight() != 3) {
            return false;
        }

        return isSpiderEye(get(input, 0))
                && isStrongPoisonPotion(get(input, 1))
                && isSpiderEye(get(input, 2))
                && isStrongPoisonPotion(get(input, 3))
                && isPufferfish(get(input, 4))
                && isStrongPoisonPotion(get(input, 5))
                && isSpiderEye(get(input, 6))
                && isStrongPoisonPotion(get(input, 7))
                && isSpiderEye(get(input, 8));
    }

    @Override
    public ItemStack craft(CraftingRecipeInput input, RegistryWrapper.WrapperLookup registries) {
        return new ItemStack(ScytheMod.TOXIC_ESSENCE);
    }

    public boolean fits(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public RecipeSerializer<? extends SpecialCraftingRecipe> getSerializer() {
        return ScytheMod.TOXIC_ESSENCE_RECIPE_SERIALIZER;
    }

    private static ItemStack get(CraftingRecipeInput input, int slot) {
        return input.getStackInSlot(slot % 3, slot / 3);
    }

    private static boolean isSpiderEye(ItemStack stack) {
        return stack.isOf(Items.SPIDER_EYE);
    }

    private static boolean isPufferfish(ItemStack stack) {
        return stack.isOf(Items.PUFFERFISH);
    }

    private static boolean isStrongPoisonPotion(ItemStack stack) {
        if (!stack.isOf(Items.POTION) && !stack.isOf(Items.SPLASH_POTION) && !stack.isOf(Items.LINGERING_POTION)) {
            return false;
        }

        PotionContentsComponent contents = stack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);
        return contents.matches(Potions.STRONG_POISON);
    }
}
