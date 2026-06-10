package com.shipovskijkorp.scythes.mod.recipe;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.recipe.CraftingRecipeCategory;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class ToxicEssenceRecipe extends SpecialCraftingRecipe {

    public ToxicEssenceRecipe(Identifier id, CraftingRecipeCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingInventory inventory, World world) {
        if (inventory.getWidth() != 3 || inventory.getHeight() != 3) {
            return false;
        }

        return isSpiderEye(inventory.getStack(0))
                && isStrongPoisonPotion(inventory.getStack(1))
                && isSpiderEye(inventory.getStack(2))
                && isStrongPoisonPotion(inventory.getStack(3))
                && isPufferfish(inventory.getStack(4))
                && isStrongPoisonPotion(inventory.getStack(5))
                && isSpiderEye(inventory.getStack(6))
                && isStrongPoisonPotion(inventory.getStack(7))
                && isSpiderEye(inventory.getStack(8));
    }

    @Override
    public ItemStack craft(CraftingInventory inventory, DynamicRegistryManager registryManager) {
        return new ItemStack(ScytheMod.TOXIC_ESSENCE);
    }

    @Override
    public boolean fits(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public ItemStack getOutput(DynamicRegistryManager registryManager) {
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
        return PotionUtil.getPotion(stack) == Potions.STRONG_POISON;
    }
}
