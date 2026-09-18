package com.shipovskijkorp.scythes.mod.util;

/** Additional data carried by every living-entity render state. */
public interface FreezingRenderState {
    boolean scythes$isFrozenForRendering();

    void scythes$setFrozenForRendering(boolean frozen);
}
