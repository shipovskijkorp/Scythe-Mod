package com.shipovskijkorp.scythes.mod.compat.jei;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

public final class ToxicEssenceJeiRecipe {

    private static final ItemStack STRONG_POISON_POTION = potion(Items.POTION);
    private static final ItemStack STRONG_POISON_SPLASH_POTION = potion(Items.SPLASH_POTION);
    private static final ItemStack STRONG_POISON_LINGERING_POTION = potion(Items.LINGERING_POTION);

    private final List<ItemStack> strongPoisonPotions;
    private final ItemStack output;

    public ToxicEssenceJeiRecipe() {
        this.strongPoisonPotions = List.of(
                STRONG_POISON_POTION.copy(),
                STRONG_POISON_SPLASH_POTION.copy(),
                STRONG_POISON_LINGERING_POTION.copy()
        );
        this.output = new ItemStack(ScytheMod.TOXIC_ESSENCE);
    }

    private static ItemStack potion(Item item) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.STRONG_POISON));
        return stack;
    }

    public ItemStack getSpiderEye() {
        return new ItemStack(Items.SPIDER_EYE);
    }

    public List<ItemStack> getStrongPoisonPotions() {
        return strongPoisonPotions;
    }

    public ItemStack getPufferfish() {
        return new ItemStack(Items.PUFFERFISH);
    }

    public ItemStack getOutput() {
        return output.copy();
    }
}
