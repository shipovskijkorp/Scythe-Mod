package com.shipovskijkorp.scythes.mod.recipe;

import com.mojang.serialization.MapCodec;
import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class ToxicEssenceRecipe extends CustomRecipe {

    public static final MapCodec<ToxicEssenceRecipe> CODEC = MapCodec.unit(new ToxicEssenceRecipe());
    public static final StreamCodec<RegistryFriendlyByteBuf, ToxicEssenceRecipe> STREAM_CODEC = StreamCodec.unit(new ToxicEssenceRecipe());

    public ToxicEssenceRecipe() {
        super();
    }

    @Override
    public boolean matches(CraftingInput input, Level world) {
        if (input.width() != 3 || input.height() != 3) {
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
    public ItemStack assemble(CraftingInput input) {
        return new ItemStack(ScytheMod.TOXIC_ESSENCE, ScytheBalance.Crafting.TOXIC_ESSENCE_OUTPUT_COUNT);
    }

    public ItemStack getResultItem() {
        return new ItemStack(ScytheMod.TOXIC_ESSENCE, ScytheBalance.Crafting.TOXIC_ESSENCE_OUTPUT_COUNT);
    }

    @Override
    public RecipeSerializer<ToxicEssenceRecipe> getSerializer() {
        return ScytheMod.TOXIC_ESSENCE_RECIPE_SERIALIZER;
    }

    private static ItemStack get(CraftingInput input, int slot) {
        return input.getItem(slot);
    }

    private static boolean isSpiderEye(ItemStack stack) {
        return stack.is(Items.SPIDER_EYE);
    }

    private static boolean isPufferfish(ItemStack stack) {
        return stack.is(Items.PUFFERFISH);
    }

    private static boolean isStrongPoisonPotion(ItemStack stack) {
        if (!stack.is(Items.POTION) && !stack.is(Items.SPLASH_POTION) && !stack.is(Items.LINGERING_POTION)) {
            return false;
        }

        PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        return contents.is(Potions.STRONG_POISON);
    }
}
