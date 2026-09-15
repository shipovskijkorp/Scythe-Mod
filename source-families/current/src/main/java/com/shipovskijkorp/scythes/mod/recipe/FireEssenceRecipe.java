package com.shipovskijkorp.scythes.mod.recipe;

import com.mojang.serialization.MapCodec;
import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class FireEssenceRecipe extends CustomRecipe {

    public static final MapCodec<FireEssenceRecipe> CODEC = MapCodec.unit(new FireEssenceRecipe());
    public static final StreamCodec<RegistryFriendlyByteBuf, FireEssenceRecipe> STREAM_CODEC = StreamCodec.unit(new FireEssenceRecipe());

    public FireEssenceRecipe() {
        super();
    }

    @Override
    public boolean matches(CraftingInput input, Level world) {
        if (input.width() != 3 || input.height() != 3) {
            return false;
        }

        return get(input, 0).is(Items.BLAZE_POWDER)
                && get(input, 1).is(Items.FIRE_CHARGE)
                && get(input, 2).is(Items.BLAZE_POWDER)
                && get(input, 3).is(Items.LAVA_BUCKET)
                && get(input, 4).is(Items.TNT)
                && get(input, 5).is(Items.LAVA_BUCKET)
                && isPlainFlightOneFirework(get(input, 6))
                && isPlainFlightOneFirework(get(input, 7))
                && isPlainFlightOneFirework(get(input, 8));
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return new ItemStack(ScytheMod.FIRE_ESSENCE, ScytheBalance.Crafting.FIRE_ESSENCE_OUTPUT_COUNT);
    }

    public ItemStack getResultItem() {
        return new ItemStack(ScytheMod.FIRE_ESSENCE, ScytheBalance.Crafting.FIRE_ESSENCE_OUTPUT_COUNT);
    }

    @Override
    public RecipeSerializer<FireEssenceRecipe> getSerializer() {
        return ScytheMod.FIRE_ESSENCE_RECIPE_SERIALIZER;
    }

    private static ItemStack get(CraftingInput input, int slot) {
        return input.getItem(slot);
    }

    private static boolean isPlainFlightOneFirework(ItemStack stack) {
        if (!stack.is(Items.FIREWORK_ROCKET)) {
            return false;
        }

        Fireworks fireworks = stack.get(DataComponents.FIREWORKS);
        return fireworks != null && fireworks.flightDuration() == 1 && fireworks.explosions().isEmpty();
    }
}
