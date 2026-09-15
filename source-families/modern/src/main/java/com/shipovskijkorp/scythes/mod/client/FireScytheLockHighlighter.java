package com.shipovskijkorp.scythes.mod.client;

import com.shipovskijkorp.scythes.mod.item.FireScytheItem;
import com.shipovskijkorp.scythes.mod.util.FireTargeting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

/** Client-only lock state used to outline the target while the Fire Scythe is being drawn. */
public final class FireScytheLockHighlighter {
    private static LivingEntity lockedTarget;
    private static boolean wasDrawing;

    private FireScytheLockHighlighter() {}

    public static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) { clear(); return; }
        boolean drawing = client.player.isUsingItem()
                && client.player.getActiveItem().getItem() instanceof FireScytheItem;
        if (!drawing) { clear(); return; }
        if (!wasDrawing) lockedTarget = FireTargeting.findLockTarget(client.player);
        wasDrawing = true;
        if (lockedTarget != null && !lockedTarget.isAlive()) lockedTarget = null;
        if (lockedTarget != null) {
//? if >=1.21.11 {
            if (lockedTarget.getEntityWorld() != client.world) lockedTarget = null;
//? } else {
            if (lockedTarget.getWorld() != client.world) lockedTarget = null;
//? }
        }
    }

    public static boolean isLocked(Entity entity) { return entity == lockedTarget; }

    private static void clear() { lockedTarget = null; wasDrawing = false; }
}
