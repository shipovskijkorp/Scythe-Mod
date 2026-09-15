package com.shipovskijkorp.scythes.mod.client.guide;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.guide.GuideDocument;
import com.shipovskijkorp.scythes.mod.guide.GuideResources;
import com.shipovskijkorp.scythes.mod.guide.GuideUi;
import com.shipovskijkorp.scythes.mod.guide.GuideViewModel;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
//? if >=1.21.11 {
import net.minecraft.client.gui.Click;
import net.minecraft.client.gl.RenderPipelines;
//? }
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Compact BCCE-style Guide Book screen for the modern family. */
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
        super(Text.literal("Welcome"));
        this.view = new GuideViewModel(resources);
    }

    public static void open() {
        MinecraftClient client = MinecraftClient.getInstance();
        GuideResources resources;
        try {
            resources = GuideResources.load(GuideResources.classpathReader(), client.getLanguageManager().getLanguage());
        } catch (Exception exception) {
            ScytheMod.LOGGER.error("Failed to load ScytheMod Guide Book", exception);
            resources = GuideResources.fallback();
        }
        client.setScreen(new GuideScreen(resources));
    }

    private static Identifier id(String path) {
        return Identifier.of(ScytheMod.MOD_ID, path);
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
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        clickRegions.clear();
        hoveredRecipeName = null;

        drawTexture(context, LEFT_PAGE, left, top, 0, 0, GuideUi.PAGE_TEXTURE_WIDTH, GuideUi.PAGE_TEXTURE_HEIGHT);
        drawTexture(context, RIGHT_PAGE, left + GuideUi.PAGE_TEXTURE_WIDTH, top, 0, 0,
            GuideUi.PAGE_TEXTURE_WIDTH, GuideUi.PAGE_TEXTURE_HEIGHT);

        renderTabs(context, mouseX, mouseY);
        int firstPage = view.firstPage();
        renderDocumentPage(context, view.document().page(firstPage), left + GuideUi.LEFT_TEXT_OFFSET,
            top + GuideUi.PAGE_TEXT_TOP, mouseX, mouseY);
        renderDocumentPage(context, view.document().page(firstPage + 1), left + GuideUi.RIGHT_TEXT_OFFSET,
            top + GuideUi.PAGE_TEXT_TOP, mouseX, mouseY);
        renderNavigation(context, mouseX, mouseY);
        renderRecipeTooltip(context, mouseX, mouseY);
        super.render(context, mouseX, mouseY, delta);
    }

    private void renderTabs(DrawContext context, int mouseX, int mouseY) {
        renderTab(context, mouseX, mouseY, 0, "Welcome", view.index() == 0, view::welcome);
        List<GuideResources.Entry> entries = view.resources().entries();
        for (int i = 0; i < entries.size(); i++) {
            GuideResources.Entry entry = entries.get(i);
            String label = view.resources().document(entry.page()).title();
            final int entryIndex = i;
            renderTab(context, mouseX, mouseY, i + 1, label, view.index() == i + 1,
                () -> view.openEntry(entryIndex));
        }
    }

    private void renderTab(DrawContext context, int mouseX, int mouseY, int row, String rawLabel,
        boolean selected, Runnable action) {
        int right = left + GuideUi.TAB_BOOK_OVERLAP;
        int y = top + GuideUi.TAB_TOP + row * GuideUi.TAB_STEP;
        int fullX = right - GuideUi.TAB_TEXTURE_WIDTH;
        boolean hovered = inside(mouseX, mouseY, fullX, y, GuideUi.TAB_TEXTURE_WIDTH, GuideUi.TAB_HEIGHT);
        int drawWidth = selected || hovered ? GuideUi.TAB_TEXTURE_WIDTH : GuideUi.TAB_NORMAL_WIDTH;
        int x = right - drawWidth;
        drawTexture(context, STYLE, x, y, GuideUi.STYLE_TAB_U, GuideUi.styleRow(row), drawWidth, GuideUi.TAB_HEIGHT);

        String label = textRenderer.trimToWidth(rawLabel, drawWidth - GuideUi.TAB_TEXT_PADDING * 2);
        int textX = x + GuideUi.TAB_TEXT_PADDING;
        int textY = y + 4;
        context.drawText(textRenderer, label, textX, textY, GuideUi.opaque(GuideUi.TEXT), false);
        context.fill(textX, textY + 9, textX + Math.min(textRenderer.getWidth(label), drawWidth - 16), textY + 10,
            GuideUi.opaque(GuideUi.TEXT));
        clickRegions.add(new ClickRegion(fullX, y, GuideUi.TAB_TEXTURE_WIDTH, GuideUi.TAB_HEIGHT, action));
    }

    private void renderDocumentPage(DrawContext context, GuideDocument.Page page, int x, int startY,
        int mouseX, int mouseY) {
        int y = startY;
        int bottom = top + GuideUi.PAGE_TEXT_TOP + GuideUi.PAGE_TEXT_HEIGHT;
        for (GuideDocument.Block block : page.blocks()) {
            if (y >= bottom) break;
            switch (block.type()) {
                case TITLE -> y = renderChapter(context, block.text(), x, y);
                case HEADING -> y = renderChapter(context, block.text(), x, y);
                case PARAGRAPH -> y = renderWrapped(context, block.text(), x, y, bottom);
                case SPACE -> y += 6;
                case SCYTHE_INDEX -> y = renderScytheIndex(context, x, y, mouseX, mouseY, bottom);
                case RECIPE -> y = renderRecipe(context, block.text(), x, y, mouseX, mouseY, bottom);
            }
        }
    }

    private int renderWrapped(DrawContext context, String text, int x, int y, int bottom) {
        List<OrderedText> lines = textRenderer.wrapLines(Text.literal(text), GuideUi.PAGE_TEXT_WIDTH);
        for (OrderedText line : lines) {
            if (y + 9 > bottom) break;
            context.drawText(textRenderer, line, x, y, GuideUi.opaque(GuideUi.TEXT), false);
            y += 10;
        }
        return y + 4;
    }


    private int renderRecipe(DrawContext context, String recipeId, int x, int y, int mouseX, int mouseY, int bottom) {
        GuideResources.GuideRecipe recipe = view.resources().recipe(recipeId);
        if (recipe == null || y + CRAFTING_GRID_HEIGHT > bottom) return y;
        int recipeX = x + (GuideUi.PAGE_TEXT_WIDTH - CRAFTING_GRID_WIDTH) / 2;
        drawTexture(context, ICONS, recipeX, y, CRAFTING_GRID_U, CRAFTING_GRID_V, CRAFTING_GRID_WIDTH, CRAFTING_GRID_HEIGHT);
        for (int i = 0; i < Math.min(9, recipe.ingredients().size()); i++) {
            GuideResources.RecipeSlot slot = recipe.ingredients().get(i);
            renderRecipeSlot(context, slot, recipeX + 1 + (i % 3) * 18, y + 1 + (i / 3) * 18, mouseX, mouseY);
        }
        renderRecipeSlot(context, recipe.output(), recipeX + 95, y + 19, mouseX, mouseY);
        return y + CRAFTING_GRID_HEIGHT + 7;
    }

    private void renderRecipeSlot(DrawContext context, GuideResources.RecipeSlot slot, int x, int y, int mouseX, int mouseY) {
        if (slot == null || slot.empty()) return;
        ItemStack stack = recipeStack(slot.item());
        if (!stack.isEmpty()) context.drawItem(stack, x, y);
        if (inside(mouseX, mouseY, x, y, 16, 16)) hoveredRecipeName = slot.name();
    }

    private static ItemStack recipeStack(String itemId) {
        if (itemId == null || itemId.isBlank()) return ItemStack.EMPTY;
        String[] parts = itemId.split(":", 2);
        if (parts.length != 2) return ItemStack.EMPTY;
        Item item = Registries.ITEM.get(Identifier.of(parts[0], parts[1]));
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    private void renderNavigation(DrawContext context, int mouseX, int mouseY) {
        int navY = top + GuideUi.PAGE_TEXT_TOP + GuideUi.PAGE_TEXT_HEIGHT;
        if (view.canPreviousSpread()) {
            int x = left + 23;
            boolean hovered = inside(mouseX, mouseY, x - 3, navY - 4, 24, 18);
            drawPageArrow(context, x, navY, false, hovered);
            clickRegions.add(new ClickRegion(x - 3, navY - 4, 24, 18, view::previousSpread));
        }
        if (view.canNextSpread()) {
            int x = left + GuideUi.PAGE_TEXTURE_WIDTH + 4 + GuideUi.PAGE_TEXT_WIDTH - 18;
            boolean hovered = inside(mouseX, mouseY, x - 3, navY - 4, 24, 18);
            drawPageArrow(context, x, navY, true, hovered);
            clickRegions.add(new ClickRegion(x - 3, navY - 4, 24, 18, view::nextSpread));
        }
        int count = view.document().pageCount();
        int first = view.firstPage();
        drawPageNumber(context, left + GuideUi.LEFT_TEXT_OFFSET, navY + 6, first, count);
        drawPageNumber(context, left + GuideUi.RIGHT_TEXT_OFFSET, navY + 6, first + 1, count);
    }

    private void drawPageArrow(DrawContext context, int x, int y, boolean forward, boolean hovered) {
        drawTexture(context, ICONS, x, y, forward ? 0 : 23, hovered ? 152 : 139, 18, 10);
    }

    private void drawPageNumber(DrawContext context, int x, int y, int page, int count) {
        if (page < 0 || page >= count) return;
        String text = (page + 1) + " / " + count;
        context.drawText(textRenderer, text, x + (GuideUi.PAGE_TEXT_WIDTH - textRenderer.getWidth(text)) / 2, y, GuideUi.MUTED, false);
    }

    private void renderRecipeTooltip(DrawContext context, int mouseX, int mouseY) {
        if (hoveredRecipeName == null || hoveredRecipeName.isBlank()) return;
        int boxWidth = textRenderer.getWidth(hoveredRecipeName) + 8;
        int boxHeight = 14;
        int x = Math.min(mouseX + 10, width - boxWidth - 4);
        int y = Math.min(mouseY + 10, height - boxHeight - 4);
        context.fill(x, y, x + boxWidth, y + boxHeight, 0xF0100010);
        context.fill(x, y, x + boxWidth, y + 1, 0xFF503A70);
        context.drawText(textRenderer, hoveredRecipeName, x + 4, y + 3, 0xFFFFFF, false);
    }
    private int renderScytheIndex(DrawContext context, int x, int y, int mouseX, int mouseY, int bottom) {
        List<GuideResources.Entry> entries = view.resources().entries();
        for (int i = 0; i < entries.size(); i++) {
            if (y + INDEX_ROW_HEIGHT > bottom) break;
            GuideResources.Entry entry = entries.get(i);
            boolean hovered = inside(mouseX, mouseY, x, y, GuideUi.PAGE_TEXT_WIDTH, INDEX_ROW_HEIGHT);
            if (hovered) context.fill(x, y, x + GuideUi.PAGE_TEXT_WIDTH, y + INDEX_ROW_HEIGHT, GuideUi.HOVER);

            Identifier texture = itemTexture(entry.item());
            if (texture != null) {
                drawTexture(context, texture, x + 2, y + 2, 0, 0, INDEX_ICON_SIZE, INDEX_ICON_SIZE,
                    INDEX_ICON_SIZE, INDEX_ICON_SIZE);
            }

            String label = view.resources().document(entry.page()).title();
            context.drawText(textRenderer, label, x + 25, y + 6, GuideUi.opaque(GuideUi.TEXT), false);
            final int entryIndex = i;
            clickRegions.add(new ClickRegion(x, y, GuideUi.PAGE_TEXT_WIDTH, INDEX_ROW_HEIGHT,
                () -> view.openEntry(entryIndex)));
            y += INDEX_ROW_HEIGHT;
        }
        return y;
    }

    private int renderChapter(DrawContext context, String title, int x, int y) {
        int styleV = GuideUi.styleRow(view.index());
        drawTexture(context, STYLE, x + 7, y - 4, GuideUi.STYLE_CHAPTER_U, styleV,
            GuideUi.STYLE_CHAPTER_WIDTH, GuideUi.TAB_HEIGHT);
        int textX = x + 16;
        context.drawText(textRenderer, title, textX, y, GuideUi.opaque(GuideUi.TEXT), false);
        context.fill(textX, y + 9,
            textX + Math.min(textRenderer.getWidth(title), GuideUi.PAGE_TEXT_WIDTH - 34), y + 10,
            GuideUi.opaque(GuideUi.TEXT));
        return y + 17;
    }

    private static Identifier itemTexture(String itemId) {
        if (itemId == null || itemId.isBlank()) return null;
        String[] parts = itemId.split(":", 2);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) return null;
        return Identifier.of(parts[0], "textures/item/" + parts[1] + ".png");
    }

    private static void drawTexture(DrawContext context, Identifier texture, int x, int y, float u, float v,
        int width, int height) {
        drawTexture(context, texture, x, y, u, v, width, height, 256, 256);
    }

    private static void drawTexture(DrawContext context, Identifier texture, int x, int y, float u, float v,
        int width, int height, int textureWidth, int textureHeight) {
//? if >=1.21.11 {
        context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, textureWidth, textureHeight);
//? } else {
        context.drawTexture(texture, x, y, u, v, width, height, textureWidth, textureHeight);
//? }
    }

//? if >=1.21.11 {
    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0 && handleClick(click.x(), click.y())) return true;
        return super.mouseClicked(click, doubled);
    }
//? } else {
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && handleClick(mouseX, mouseY)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }
//? }

    private boolean handleClick(double mouseX, double mouseY) {
        for (ClickRegion region : clickRegions) {
            if (region.contains(mouseX, mouseY)) {
                region.action.run();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean shouldPause() {
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
