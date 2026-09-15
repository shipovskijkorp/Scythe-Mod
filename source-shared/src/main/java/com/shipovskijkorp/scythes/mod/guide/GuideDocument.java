package com.shipovskijkorp.scythes.mod.guide;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Very small Markdown-like document used by the ScytheMod guide renderer. */
public final class GuideDocument {
    private final String title;
    private final List<Page> pages;

    private GuideDocument(String title, List<Page> pages) {
        this.title = title;
        this.pages = Collections.unmodifiableList(new ArrayList<>(pages));
    }

    public static GuideDocument parse(String source) {
        List<Page> pages = new ArrayList<>();
        List<Block> current = new ArrayList<>();
        String title = "";
        String[] lines = (source == null ? "" : source).replace("\r", "").split("\n", -1);
        for (String raw : lines) {
            String line = raw.trim();
            if (line.equals("<new_page/>")) {
                pages.add(new Page(current));
                current = new ArrayList<>();
                continue;
            }
            if (line.startsWith("# ")) {
                String value = line.substring(2).trim();
                if (title.isEmpty()) title = value;
                current.add(new Block(BlockType.TITLE, value));
            } else if (line.startsWith("## ")) {
                current.add(new Block(BlockType.HEADING, line.substring(3).trim()));
            } else if (line.equals("<scythe_index/>")) {
                current.add(new Block(BlockType.SCYTHE_INDEX, ""));
            } else if (line.startsWith("<recipe:") && line.endsWith("/>")) {
                String recipe = line.substring(8, line.length() - 2).trim();
                if (!recipe.isEmpty()) current.add(new Block(BlockType.RECIPE, recipe));
            } else if (line.isEmpty()) {
                current.add(new Block(BlockType.SPACE, ""));
            } else {
                current.add(new Block(BlockType.PARAGRAPH, raw.trim()));
            }
        }
        pages.add(new Page(current));
        while (pages.size() < 2) pages.add(new Page(List.of()));
        return new GuideDocument(title, pages);
    }

    public static GuideDocument empty(String title) {
        return new GuideDocument(title == null ? "" : title, List.of(new Page(List.of()), new Page(List.of())));
    }

    public String title() {
        return title;
    }

    public Page page(int index) {
        return index >= 0 && index < pages.size() ? pages.get(index) : new Page(List.of());
    }

    public int pageCount() {
        return pages.size();
    }

    public record Page(List<Block> blocks) {
        public Page {
            blocks = Collections.unmodifiableList(new ArrayList<>(blocks));
        }
    }

    public record Block(BlockType type, String text) {}

    public enum BlockType {
        TITLE,
        HEADING,
        PARAGRAPH,
        SPACE,
        SCYTHE_INDEX,
        RECIPE
    }
}
