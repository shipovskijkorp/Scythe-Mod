package com.shipovskijkorp.scythes.mod.compat.jei;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class ToxicEssenceRecipeCategory implements IRecipeCategory<ToxicEssenceJeiRecipe> {

    public static final RecipeType<ToxicEssenceJeiRecipe> RECIPE_TYPE = RecipeType.create(
            ScytheMod.MOD_ID,
            "toxic_essence",
            ToxicEssenceJeiRecipe.class
    );

    private static final int SLOT_SIZE = 18;
    private static final int GRID_LEFT = 0;
    private static final int GRID_TOP = 0;
    private static final int OUTPUT_LEFT = 94;
    private static final int OUTPUT_TOP = 18;
    private static final int WIDTH = 116;
    private static final int HEIGHT = 54;

    private final IDrawable icon;
    private final IDrawable slotBackground;

    public ToxicEssenceRecipeCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ScytheMod.TOXIC_ESSENCE));
        this.slotBackground = guiHelper.getSlotDrawable();
    }

    @Override
    public RecipeType<ToxicEssenceJeiRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.scythes.category.toxic_essence");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ToxicEssenceJeiRecipe recipe, IFocusGroup focuses) {
        addSpiderEye(builder, recipe, 0, 0);
        addPoisonPotion(builder, recipe, 1, 0);
        addSpiderEye(builder, recipe, 2, 0);

        addPoisonPotion(builder, recipe, 0, 1);
        addPufferfish(builder, recipe, 1, 1);
        addPoisonPotion(builder, recipe, 2, 1);

        addSpiderEye(builder, recipe, 0, 2);
        addPoisonPotion(builder, recipe, 1, 2);
        addSpiderEye(builder, recipe, 2, 2);

        addSlot(builder, RecipeIngredientRole.OUTPUT, OUTPUT_LEFT + 1, OUTPUT_TOP + 1)
                .addItemStack(recipe.getOutput());
    }

    private mezz.jei.api.gui.builder.IRecipeSlotBuilder addSlot(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, int y) {
        return builder.addSlot(role, x, y)
                .setBackground(slotBackground, -1, -1);
    }

    private void addSpiderEye(IRecipeLayoutBuilder builder, ToxicEssenceJeiRecipe recipe, int gridX, int gridY) {
        addSlot(builder, RecipeIngredientRole.INPUT, slotX(gridX), slotY(gridY))
                .addItemStack(recipe.getSpiderEye());
    }

    private void addPoisonPotion(IRecipeLayoutBuilder builder, ToxicEssenceJeiRecipe recipe, int gridX, int gridY) {
        addSlot(builder, RecipeIngredientRole.INPUT, slotX(gridX), slotY(gridY))
                .addItemStacks(recipe.getStrongPoisonPotions());
    }

    private void addPufferfish(IRecipeLayoutBuilder builder, ToxicEssenceJeiRecipe recipe, int gridX, int gridY) {
        addSlot(builder, RecipeIngredientRole.INPUT, slotX(gridX), slotY(gridY))
                .addItemStack(recipe.getPufferfish());
    }

    private static int slotX(int gridX) {
        return GRID_LEFT + gridX * SLOT_SIZE + 1;
    }

    private static int slotY(int gridY) {
        return GRID_TOP + gridY * SLOT_SIZE + 1;
    }
}
