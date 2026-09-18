package com.shipovskijkorp.scythes.mod.compat.jei;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import java.util.List;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potions;

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
        stack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Potions.STRONG_POISON));
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
