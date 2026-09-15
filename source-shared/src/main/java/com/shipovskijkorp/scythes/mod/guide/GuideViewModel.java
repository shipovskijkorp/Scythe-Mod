package com.shipovskijkorp.scythes.mod.guide;

import java.util.List;

/** Loader-neutral bookmark and in-section spread navigation state. */
public final class GuideViewModel {
    private final GuideResources resources;
    private int index;
    private int spread;

    public GuideViewModel(GuideResources resources) {
        this.resources = resources;
    }

    public GuideResources resources() { return resources; }
    public int index() { return index; }
    public int spread() { return spread; }

    public GuideDocument document() { return resources.document(pageId()); }

    public String pageId() {
        if (index <= 0) return resources.welcomePage();
        List<GuideResources.Entry> entries = resources.entries();
        int entryIndex = index - 1;
        if (entryIndex < 0 || entryIndex >= entries.size()) return resources.welcomePage();
        return entries.get(entryIndex).page();
    }

    public int firstPage() { return spread * 2; }
    public int maxSpread() { return Math.max(0, (document().pageCount() - 1) / 2); }
    public boolean canPreviousSpread() { return spread > 0; }
    public boolean canNextSpread() { return spread < maxSpread(); }

    public void previousSpread() { if (canPreviousSpread()) spread--; }
    public void nextSpread() { if (canNextSpread()) spread++; }

    public void welcome() { index = 0; spread = 0; }

    public void openEntry(int entryIndex) {
        if (entryIndex >= 0 && entryIndex < resources.entries().size()) {
            index = entryIndex + 1;
            spread = 0;
        }
    }
}
