package com.shipovskijkorp.scythes.mod.compat.jei;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

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

    private final IDrawable background;
    private final IDrawable icon;

    public ToxicEssenceRecipeCategory(IGuiHelper guiHelper) {
//? if >=1.21.11 {
        this.background = guiHelper.createDrawable(Identifier.of("minecraft", "textures/gui/container/crafting_table.png"), 29, 16, WIDTH, HEIGHT);
//? } else {
        this.background = guiHelper.createDrawable(Identifier.of("minecraft", "textures/gui/container/crafting_table.png"), 29, 16, 116, 54);
//? }
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ScytheMod.TOXIC_ESSENCE));
    }

    @Override
    public RecipeType<ToxicEssenceJeiRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Text getTitle() {
        return Text.translatable("jei.scythes.category.toxic_essence");
    }

    @Override
//? if >=1.21.11 {
    public IDrawable getIcon() {
        return icon;
    }

    @Override
//? } else {
//? }
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
//? if >=1.21.11 {
    public void draw(ToxicEssenceJeiRecipe recipe, IRecipeSlotsView recipeSlotsView, DrawContext guiGraphics, double mouseX, double mouseY) {
        background.draw(guiGraphics);
//? } else {
    public void draw(ToxicEssenceJeiRecipe recipe, IRecipeSlotsView recipeSlotsView, DrawContext context, double mouseX, double mouseY) {
        background.draw(context);
    }

    @Override
    public IDrawable getIcon() {
        return icon;
//? }
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

        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_LEFT + 1, OUTPUT_TOP + 1)
                .addItemStack(recipe.getOutput());
    }

    private static void addSpiderEye(IRecipeLayoutBuilder builder, ToxicEssenceJeiRecipe recipe, int gridX, int gridY) {
        builder.addSlot(RecipeIngredientRole.INPUT, slotX(gridX), slotY(gridY))
                .addItemStack(recipe.getSpiderEye());
    }

    private static void addPoisonPotion(IRecipeLayoutBuilder builder, ToxicEssenceJeiRecipe recipe, int gridX, int gridY) {
        builder.addSlot(RecipeIngredientRole.INPUT, slotX(gridX), slotY(gridY))
                .addItemStacks(recipe.getStrongPoisonPotions());
    }

    private static void addPufferfish(IRecipeLayoutBuilder builder, ToxicEssenceJeiRecipe recipe, int gridX, int gridY) {
        builder.addSlot(RecipeIngredientRole.INPUT, slotX(gridX), slotY(gridY))
                .addItemStack(recipe.getPufferfish());
    }

    private static int slotX(int gridX) {
        return GRID_LEFT + gridX * SLOT_SIZE + 1;
    }

    private static int slotY(int gridY) {
        return GRID_TOP + gridY * SLOT_SIZE + 1;
    }
}
