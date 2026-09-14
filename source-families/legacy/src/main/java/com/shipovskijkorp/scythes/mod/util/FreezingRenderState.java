package com.shipovskijkorp.scythes.mod.util;

/**
 * Client-visible state synchronized through the entity data tracker.
 *
 * Status effects normally synchronize to clients, but the explicit flag keeps
 * the visual shell independent from status-effect packet timing for non-player
 * living entities.
 */
public interface FreezingRenderState {

    boolean scythes$isFrozenForRendering();
}
