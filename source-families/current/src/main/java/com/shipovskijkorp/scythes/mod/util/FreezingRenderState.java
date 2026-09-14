package com.shipovskijkorp.scythes.mod.util;

/** Extra synchronized state used by the frozen-entity overlay. */
public interface FreezingRenderState {
    boolean scythes$isFrozenForRendering();
    void scythes$setFrozenForRendering(boolean frozen);
}
