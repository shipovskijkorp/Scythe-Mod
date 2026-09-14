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
    public boolean matches(CraftingRecipeInput inventory, World world) {
        if (inventory.getWidth() != 3 || inventory.getHeight() != 3) {
            return false;
        }

        return isSpiderEye(inventory.getStackInSlot(0, 0))
                && isStrongPoisonPotion(inventory.getStackInSlot(1, 0))
                && isSpiderEye(inventory.getStackInSlot(2, 0))
                && isStrongPoisonPotion(inventory.getStackInSlot(0, 1))
                && isPufferfish(inventory.getStackInSlot(1, 1))
                && isStrongPoisonPotion(inventory.getStackInSlot(2, 1))
                && isSpiderEye(inventory.getStackInSlot(0, 2))
                && isStrongPoisonPotion(inventory.getStackInSlot(1, 2))
                && isSpiderEye(inventory.getStackInSlot(2, 2));
    }

    @Override
    public ItemStack craft(CraftingRecipeInput inventory, RegistryWrapper.WrapperLookup registries) {
        return new ItemStack(ScytheMod.TOXIC_ESSENCE);
    }

    @Override
    public boolean fits(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public ItemStack getResult(RegistryWrapper.WrapperLookup registries) {
        return new ItemStack(ScytheMod.TOXIC_ESSENCE);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ScytheMod.TOXIC_ESSENCE_RECIPE_SERIALIZER;
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
        PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
        return contents != null && contents.potion().isPresent() && contents.potion().get().equals(Potions.STRONG_POISON);
    }
}
