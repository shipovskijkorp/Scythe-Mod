package com.shipovskijkorp.scythes.mod.compat.jei;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import java.util.List;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potions;

/**
 * Client-only display model for JEI.
 *
 * <p>The real toxic essence recipe is a {@code SpecialCraftingRecipe}, so its ingredients cannot be represented
 * by vanilla JSON ingredients well enough for JEI to infer them automatically. This tiny display model mirrors the
 * actual server-side matcher from {@link com.shipovskijkorp.scythes.mod.recipe.ToxicEssenceRecipe}.</p>
 */
public final class ToxicEssenceJeiRecipe {

    private static final ItemStack STRONG_POISON_POTION = PotionContentsComponent.createStack(Items.POTION, Potions.STRONG_POISON);
    private static final ItemStack STRONG_POISON_SPLASH_POTION = PotionContentsComponent.createStack(Items.SPLASH_POTION, Potions.STRONG_POISON);
    private static final ItemStack STRONG_POISON_LINGERING_POTION = PotionContentsComponent.createStack(Items.LINGERING_POTION, Potions.STRONG_POISON);

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
