package com.shipovskijkorp.scythes.mod.compat.jei;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class FireEssenceRecipeCategory implements IRecipeCategory<FireEssenceJeiRecipe> {

    public static final RecipeType<FireEssenceJeiRecipe> RECIPE_TYPE = RecipeType.create(
            ScytheMod.MOD_ID,
            "fire_essence",
            FireEssenceJeiRecipe.class
    );

    private static final int SLOT_SIZE = 18;
    private static final int GRID_LEFT = 0;
    private static final int GRID_TOP = 0;
    private static final int OUTPUT_LEFT = 94;
    private static final int OUTPUT_TOP = 18;

    private final IDrawable background;
    private final IDrawable icon;

    public FireEssenceRecipeCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(new Identifier("minecraft", "textures/gui/container/crafting_table.png"), 29, 16, 116, 54);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ScytheMod.FIRE_ESSENCE));
    }

    @Override
    public RecipeType<FireEssenceJeiRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Text getTitle() {
        return Text.translatable("jei.scythes.category.fire_essence");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, FireEssenceJeiRecipe recipe, IFocusGroup focuses) {
        addBlazePowder(builder, recipe, 0, 0);
        addFireCharge(builder, recipe, 1, 0);
        addBlazePowder(builder, recipe, 2, 0);

        addLavaBucket(builder, recipe, 0, 1);
        addTnt(builder, recipe, 1, 1);
        addLavaBucket(builder, recipe, 2, 1);

        addFirework(builder, recipe, 0, 2);
        addFirework(builder, recipe, 1, 2);
        addFirework(builder, recipe, 2, 2);

        builder.addOutputSlot(OUTPUT_LEFT + 1, OUTPUT_TOP + 1)
                .addItemStack(recipe.getOutput());
    }

    private static void addBlazePowder(IRecipeLayoutBuilder builder, FireEssenceJeiRecipe recipe, int x, int y) {
        builder.addInputSlot(slotX(x), slotY(y)).addItemStack(recipe.getBlazePowder());
    }

    private static void addFireCharge(IRecipeLayoutBuilder builder, FireEssenceJeiRecipe recipe, int x, int y) {
        builder.addInputSlot(slotX(x), slotY(y)).addItemStack(recipe.getFireCharge());
    }

    private static void addLavaBucket(IRecipeLayoutBuilder builder, FireEssenceJeiRecipe recipe, int x, int y) {
        builder.addInputSlot(slotX(x), slotY(y)).addItemStack(recipe.getLavaBucket());
    }

    private static void addTnt(IRecipeLayoutBuilder builder, FireEssenceJeiRecipe recipe, int x, int y) {
        builder.addInputSlot(slotX(x), slotY(y)).addItemStack(recipe.getTnt());
    }

    private static void addFirework(IRecipeLayoutBuilder builder, FireEssenceJeiRecipe recipe, int x, int y) {
        builder.addInputSlot(slotX(x), slotY(y)).addItemStack(recipe.getFlightOneFirework());
    }

    private static int slotX(int gridX) {
        return GRID_LEFT + gridX * SLOT_SIZE + 1;
    }

    private static int slotY(int gridY) {
        return GRID_TOP + gridY * SLOT_SIZE + 1;
    }
}
