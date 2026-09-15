package com.shipovskijkorp.scythes.mod.client;

import com.shipovskijkorp.scythes.mod.item.FireScytheItem;
import com.shipovskijkorp.scythes.mod.util.FireTargeting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Client-only lock state used to outline the target while the Fire Scythe is being drawn. */
public final class FireScytheLockHighlighter {
    private static LivingEntity lockedTarget;
    private static boolean wasDrawing;

    private FireScytheLockHighlighter() {}

    public static void tick(Minecraft client) {
        if (client.player == null || client.level == null) { clear(); return; }
        boolean drawing = client.player.isUsingItem()
                && client.player.getUseItem().getItem() instanceof FireScytheItem;
        if (!drawing) { clear(); return; }
        if (!wasDrawing) lockedTarget = FireTargeting.findLockTarget(client.player);
        wasDrawing = true;
        if (lockedTarget != null && (!lockedTarget.isAlive() || lockedTarget.level() != client.level)) lockedTarget = null;
    }

    public static boolean isLocked(Entity entity) { return entity == lockedTarget; }

    private static void clear() { lockedTarget = null; wasDrawing = false; }
}
