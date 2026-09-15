package com.shipovskijkorp.scythes.mod.guide;

/** Shared geometry and colours for the compact BCCE-style Guide Book client adapters. */
public final class GuideUi {
    public static final int PAGE_TEXTURE_WIDTH = 193;
    public static final int PAGE_TEXTURE_HEIGHT = 248;
    public static final int BOOK_WIDTH = PAGE_TEXTURE_WIDTH * 2;
    public static final int BOOK_HEIGHT = PAGE_TEXTURE_HEIGHT;
    public static final int PAGE_TEXT_WIDTH = 168;
    public static final int PAGE_TEXT_HEIGHT = 190;
    public static final int PAGE_TEXT_TOP = 25;
    public static final int LEFT_TEXT_OFFSET = 23;
    public static final int RIGHT_TEXT_OFFSET = PAGE_TEXTURE_WIDTH + 4;

    /** Exact BCCE chapter-tab silhouette, packed as a pre-tinted atlas to avoid loader-specific shader code. */
    public static final int TAB_TEXTURE_WIDTH = 104;
    public static final int TAB_NORMAL_WIDTH = 99;
    public static final int TAB_HEIGHT = 16;
    public static final int TAB_STEP = 17;
    public static final int TAB_TOP = 18;
    public static final int TAB_TEXT_PADDING = 8;
    public static final int TAB_BOOK_OVERLAP = 10;

    public static final int STYLE_TEXTURE_SIZE = 256;
    public static final int STYLE_TAB_U = 0;
    public static final int STYLE_CHAPTER_U = 112;
    public static final int STYLE_CHAPTER_WIDTH = 144;
    public static final int STYLE_ROW_HEIGHT = 16;
    public static final int STYLE_SCYTHE_ICON_V = 128;
    public static final int STYLE_SCYTHE_ICON_SIZE = 16;

    public static final int TEXT = 0x30251D;
    public static final int MUTED = 0x716355;
    public static final int CHAPTER = 0x8F6D43;
    public static final int HOVER = 0xFFD3AD6C;

    private GuideUi() {}

    public static int opaque(int rgb) {
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }

    /** 0 = Welcome, 1..N = the matching manifest entry. */
    public static int styleRow(int pageIndex) {
        return Math.max(0, pageIndex) * STYLE_ROW_HEIGHT;
    }

    public static int scytheIconU(int entryIndex) {
        return Math.max(0, entryIndex) * STYLE_SCYTHE_ICON_SIZE;
    }
}
