package com.shipovskijkorp.scythes.mod.client.guide;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.guide.GuideDocument;
import com.shipovskijkorp.scythes.mod.guide.GuideResources;
import com.shipovskijkorp.scythes.mod.guide.GuideUi;
import com.shipovskijkorp.scythes.mod.guide.GuideViewModel;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.util.FormattedCharSequence;

/** Compact BCCE-style Guide Book screen for Minecraft 26.x. */
public final class GuideScreen extends Screen {
    private static final Identifier LEFT_PAGE = id("textures/gui/guide/left_page.png");
    private static final Identifier RIGHT_PAGE = id("textures/gui/guide/right_page.png");
    private static final Identifier STYLE = id("textures/gui/guide/style.png");
    private static final Identifier ICONS = id("textures/gui/guide/icons.png");
    private static final int CRAFTING_GRID_U = 119;
    private static final int CRAFTING_GRID_V = 0;
    private static final int CRAFTING_GRID_WIDTH = 116;
    private static final int CRAFTING_GRID_HEIGHT = 54;
    private static final int INDEX_ROW_HEIGHT = 20;
    private static final int INDEX_ICON_SIZE = 16;
    
    private final GuideViewModel view;
    private final List<ClickRegion> clickRegions = new ArrayList<>();
    private int left;
    private int top;
    private String hoveredRecipeName;

    private GuideScreen(GuideResources resources) {
        super(Component.literal("Welcome"));
        this.view = new GuideViewModel(resources);
    }

    public static void open() {
        Minecraft client = Minecraft.getInstance();
        GuideResources resources;
        try {
            resources = GuideResources.load(GuideResources.classpathReader(), client.getLanguageManager().getSelected());
        } catch (Exception exception) {
            ScytheMod.LOGGER.error("Failed to load ScytheMod Guide Book", exception);
            resources = GuideResources.fallback();
        }
//? if >=26.2 {
        client.gui.setScreen(new GuideScreen(resources));
//? } else {
        client.setScreen(new GuideScreen(resources));
//? }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ScytheMod.MOD_ID, path);
    }

    @Override
    protected void init() {
        super.init();
        int tabsWidth = GuideUi.TAB_TEXTURE_WIDTH - GuideUi.TAB_BOOK_OVERLAP;
        int totalWidth = GuideUi.BOOK_WIDTH + tabsWidth;
        left = (width - totalWidth) / 2 + tabsWidth;
        top = (height - GuideUi.BOOK_HEIGHT) / 2;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        clickRegions.clear();
        hoveredRecipeName = null;

        drawTexture(graphics, LEFT_PAGE, left, top, 0, 0, GuideUi.PAGE_TEXTURE_WIDTH, GuideUi.PAGE_TEXTURE_HEIGHT);
        drawTexture(graphics, RIGHT_PAGE, left + GuideUi.PAGE_TEXTURE_WIDTH, top, 0, 0,
            GuideUi.PAGE_TEXTURE_WIDTH, GuideUi.PAGE_TEXTURE_HEIGHT);

        renderTabs(graphics, mouseX, mouseY);
        int firstPage = view.firstPage();
        renderDocumentPage(graphics, view.document().page(firstPage), left + GuideUi.LEFT_TEXT_OFFSET,
            top + GuideUi.PAGE_TEXT_TOP, mouseX, mouseY);
        renderDocumentPage(graphics, view.document().page(firstPage + 1), left + GuideUi.RIGHT_TEXT_OFFSET,
            top + GuideUi.PAGE_TEXT_TOP, mouseX, mouseY);
        renderNavigation(graphics, mouseX, mouseY);
        renderRecipeTooltip(graphics, mouseX, mouseY);
    }

    private void renderTabs(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        renderTab(graphics, mouseX, mouseY, 0, "Welcome", view.index() == 0, view::welcome);
        List<GuideResources.Entry> entries = view.resources().entries();
        for (int i = 0; i < entries.size(); i++) {
            GuideResources.Entry entry = entries.get(i);
            String label = view.resources().document(entry.page()).title();
            final int entryIndex = i;
            renderTab(graphics, mouseX, mouseY, i + 1, label, view.index() == i + 1,
                () -> view.openEntry(entryIndex));
        }
    }

    private void renderTab(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int row, String rawLabel,
        boolean selected, Runnable action) {
        int right = left + GuideUi.TAB_BOOK_OVERLAP;
        int y = top + GuideUi.TAB_TOP + row * GuideUi.TAB_STEP;
        int fullX = right - GuideUi.TAB_TEXTURE_WIDTH;
        boolean hovered = inside(mouseX, mouseY, fullX, y, GuideUi.TAB_TEXTURE_WIDTH, GuideUi.TAB_HEIGHT);
        int drawWidth = selected || hovered ? GuideUi.TAB_TEXTURE_WIDTH : GuideUi.TAB_NORMAL_WIDTH;
        int x = right - drawWidth;
        drawTexture(graphics, STYLE, x, y, GuideUi.STYLE_TAB_U, GuideUi.styleRow(row), drawWidth, GuideUi.TAB_HEIGHT);

        String label = font.plainSubstrByWidth(rawLabel, drawWidth - GuideUi.TAB_TEXT_PADDING * 2);
        int textX = x + GuideUi.TAB_TEXT_PADDING;
        int textY = y + 4;
        graphics.text(font, label, textX, textY, GuideUi.opaque(GuideUi.TEXT), false);
        graphics.fill(textX, textY + 9, textX + Math.min(font.width(label), drawWidth - 16), textY + 10,
            GuideUi.opaque(GuideUi.TEXT));
        clickRegions.add(new ClickRegion(fullX, y, GuideUi.TAB_TEXTURE_WIDTH, GuideUi.TAB_HEIGHT, action));
    }

    private void renderDocumentPage(GuiGraphicsExtractor graphics, GuideDocument.Page page, int x, int startY,
        int mouseX, int mouseY) {
        int y = startY;
        int bottom = top + GuideUi.PAGE_TEXT_TOP + GuideUi.PAGE_TEXT_HEIGHT;
        for (GuideDocument.Block block : page.blocks()) {
            if (y >= bottom) break;
            switch (block.type()) {
                case TITLE -> y = renderChapter(graphics, block.text(), x, y);
                case HEADING -> y = renderChapter(graphics, block.text(), x, y);
                case PARAGRAPH -> y = renderWrapped(graphics, block.text(), x, y, bottom);
                case SPACE -> y += 6;
                case SCYTHE_INDEX -> y = renderScytheIndex(graphics, x, y, mouseX, mouseY, bottom);
                case RECIPE -> y = renderRecipe(graphics, block.text(), x, y, mouseX, mouseY, bottom);
            }
        }
    }

    private int renderWrapped(GuiGraphicsExtractor graphics, String text, int x, int y, int bottom) {
        List<FormattedCharSequence> lines = font.split(Component.literal(text), GuideUi.PAGE_TEXT_WIDTH);
        for (FormattedCharSequence line : lines) {
            if (y + font.lineHeight > bottom) break;
            graphics.text(font, line, x, y, GuideUi.opaque(GuideUi.TEXT), false);
            y += font.lineHeight + 1;
        }
        return y + 4;
    }

    private int renderRecipe(GuiGraphicsExtractor graphics, String recipeId, int x, int y, int mouseX, int mouseY, int bottom) {
        GuideResources.GuideRecipe recipe = view.resources().recipe(recipeId);
        if (recipe == null || y + CRAFTING_GRID_HEIGHT > bottom) return y;
        int recipeX = x + (GuideUi.PAGE_TEXT_WIDTH - CRAFTING_GRID_WIDTH) / 2;
        drawTexture(graphics, ICONS, recipeX, y, CRAFTING_GRID_U, CRAFTING_GRID_V, CRAFTING_GRID_WIDTH, CRAFTING_GRID_HEIGHT);
        for (int i = 0; i < Math.min(9, recipe.ingredients().size()); i++) {
            GuideResources.RecipeSlot slot = recipe.ingredients().get(i);
            renderRecipeSlot(graphics, slot, recipeX + 1 + (i % 3) * 18, y + 1 + (i / 3) * 18, mouseX, mouseY);
        }
        renderRecipeSlot(graphics, recipe.output(), recipeX + 95, y + 19, mouseX, mouseY);
        return y + CRAFTING_GRID_HEIGHT + 7;
    }

    private void renderRecipeSlot(GuiGraphicsExtractor graphics, GuideResources.RecipeSlot slot, int x, int y, int mouseX, int mouseY) {
        if (slot == null || slot.empty()) return;
        ItemStack stack = recipeStack(slot);
        if (!stack.isEmpty()) graphics.item(stack, x, y);
        if (inside(mouseX, mouseY, x, y, 16, 16)) hoveredRecipeName = slot.name();
    }

    private static ItemStack recipeStack(GuideResources.RecipeSlot slot) {
        if (slot == null || slot.empty()) return ItemStack.EMPTY;
        if ("strong_poison".equals(slot.nameKey()) && "minecraft:potion".equals(slot.item())) {
            ItemStack stack = new ItemStack(Items.POTION);
            stack.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.STRONG_POISON));
            return stack;
        }
        return recipeStack(slot.item());
    }

    private static ItemStack recipeStack(String itemId) {
        if (itemId == null || itemId.isBlank()) return ItemStack.EMPTY;
        String[] parts = itemId.split(":", 2);
        if (parts.length != 2) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(parts[0], parts[1]));
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    private void renderNavigation(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int navY = top + GuideUi.PAGE_TEXT_TOP + GuideUi.PAGE_TEXT_HEIGHT;
        if (view.canPreviousSpread()) {
            int x = left + 23;
            boolean hovered = inside(mouseX, mouseY, x - 3, navY - 4, 24, 18);
            drawPageArrow(graphics, x, navY, false, hovered);
            clickRegions.add(new ClickRegion(x - 3, navY - 4, 24, 18, view::previousSpread));
        }
        if (view.canNextSpread()) {
            int x = left + GuideUi.PAGE_TEXTURE_WIDTH + 4 + GuideUi.PAGE_TEXT_WIDTH - 18;
            boolean hovered = inside(mouseX, mouseY, x - 3, navY - 4, 24, 18);
            drawPageArrow(graphics, x, navY, true, hovered);
            clickRegions.add(new ClickRegion(x - 3, navY - 4, 24, 18, view::nextSpread));
        }
        int count = view.document().pageCount();
        int first = view.firstPage();
        drawPageNumber(graphics, left + GuideUi.LEFT_TEXT_OFFSET, navY + 6, first, count);
        drawPageNumber(graphics, left + GuideUi.RIGHT_TEXT_OFFSET, navY + 6, first + 1, count);
    }

    private void drawPageArrow(GuiGraphicsExtractor graphics, int x, int y, boolean forward, boolean hovered) {
        drawTexture(graphics, ICONS, x, y, forward ? 0 : 23, hovered ? 152 : 139, 18, 10);
    }

    private void drawPageNumber(GuiGraphicsExtractor graphics, int x, int y, int page, int count) {
        if (page < 0 || page >= count) return;
        String text = (page + 1) + " / " + count;
        graphics.text(font, text, x + (GuideUi.PAGE_TEXT_WIDTH - font.width(text)) / 2, y, GuideUi.MUTED, false);
    }

    private void renderRecipeTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (hoveredRecipeName == null || hoveredRecipeName.isBlank()) return;
        int boxWidth = font.width(hoveredRecipeName) + 8;
        int boxHeight = 14;
        int x = Math.min(mouseX + 10, width - boxWidth - 4);
        int y = Math.min(mouseY + 10, height - boxHeight - 4);
        graphics.fill(x, y, x + boxWidth, y + boxHeight, 0xF0100010);
        graphics.fill(x, y, x + boxWidth, y + 1, 0xFF503A70);
        graphics.text(font, hoveredRecipeName, x + 4, y + 3, GuideUi.opaque(0xFFFFFF), false);
    }

    private int renderScytheIndex(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY, int bottom) {
        List<GuideResources.Entry> entries = view.resources().entries();
        for (int i = 0; i < entries.size(); i++) {
            if (y + INDEX_ROW_HEIGHT > bottom) break;
            GuideResources.Entry entry = entries.get(i);
            boolean hovered = inside(mouseX, mouseY, x, y, GuideUi.PAGE_TEXT_WIDTH, INDEX_ROW_HEIGHT);
            if (hovered) graphics.fill(x, y, x + GuideUi.PAGE_TEXT_WIDTH, y + INDEX_ROW_HEIGHT, GuideUi.HOVER);

            ItemStack icon = recipeStack(entry.item());
            if (!icon.isEmpty()) {
                graphics.item(icon, x + 2, y + 2);
            }

            String label = view.resources().document(entry.page()).title();
            graphics.text(font, label, x + 25, y + 6, GuideUi.opaque(GuideUi.TEXT), false);
            final int entryIndex = i;
            clickRegions.add(new ClickRegion(x, y, GuideUi.PAGE_TEXT_WIDTH, INDEX_ROW_HEIGHT,
                () -> view.openEntry(entryIndex)));
            y += INDEX_ROW_HEIGHT;
        }
        return y;
    }

    private int renderChapter(GuiGraphicsExtractor graphics, String title, int x, int y) {
        int styleV = GuideUi.styleRow(view.index());
        drawTexture(graphics, STYLE, x + 7, y - 4, GuideUi.STYLE_CHAPTER_U, styleV,
            GuideUi.STYLE_CHAPTER_WIDTH, GuideUi.TAB_HEIGHT);
        int textX = x + 16;
        graphics.text(font, title, textX, y, GuideUi.opaque(GuideUi.TEXT), false);
        graphics.fill(textX, y + 9,
            textX + Math.min(font.width(title), GuideUi.PAGE_TEXT_WIDTH - 34), y + 10,
            GuideUi.opaque(GuideUi.TEXT));
        return y + 17;
    }

    private static void drawTexture(GuiGraphicsExtractor graphics, Identifier texture, int x, int y,
        float u, float v, int width, int height) {
        drawTexture(graphics, texture, x, y, u, v, width, height, 256, 256);
    }

    private static void drawTexture(GuiGraphicsExtractor graphics, Identifier texture, int x, int y,
        float u, float v, int width, int height, int textureWidth, int textureHeight) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, textureWidth, textureHeight);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
//? if >=26.3 {
        if (event.button() == 1) {
//? } else {
        if (event.button() == 0) {
//? }
            for (ClickRegion region : clickRegions) {
                if (region.contains(event.x(), event.y())) {
                    region.action.run();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private record ClickRegion(int x, int y, int width, int height, Runnable action) {
        boolean contains(double mouseX, double mouseY) {
            return inside(mouseX, mouseY, x, y, width, height);
        }
    }
}
