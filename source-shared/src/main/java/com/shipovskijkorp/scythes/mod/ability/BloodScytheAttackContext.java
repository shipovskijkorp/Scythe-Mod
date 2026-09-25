package com.shipovskijkorp.scythes.mod.ability;

import java.util.UUID;

/** Synchronous marker for Blood Blender damage so offhand activations keep Blood Scythe hit semantics. */
public final class BloodScytheAttackContext {
    private BloodScytheAttackContext() {}

    private static final ThreadLocal<UUID> ACTIVE_OWNER = new ThreadLocal<>();

    public static void enter(UUID owner) {
        ACTIVE_OWNER.set(owner);
    }

    public static boolean isActive(UUID owner) {
        return owner != null && owner.equals(ACTIVE_OWNER.get());
    }

    public static void exit() {
        ACTIVE_OWNER.remove();
    }
}
