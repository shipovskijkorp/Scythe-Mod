package com.shipovskijkorp.scythes.mod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Client-side mirror of the server-authoritative Freezing visual flag. */
public final class FreezingVisualClientState {
    private static final Set<UUID> FROZEN_ENTITIES = new HashSet<>();
    private static ClientLevel lastLevel;
    private static Object lastPlayer;

    private FreezingVisualClientState() {}

    public static void setFrozen(UUID entityUuid, boolean frozen) {
        Minecraft client = Minecraft.getInstance();
        syncContext(client.level, client.player);
        if (frozen) {
            FROZEN_ENTITIES.add(entityUuid);
        } else {
            FROZEN_ENTITIES.remove(entityUuid);
        }
    }

    public static boolean isFrozen(UUID entityUuid) {
        return FROZEN_ENTITIES.contains(entityUuid);
    }

    public static void tick(Minecraft client) {
        syncContext(client.level, client.player);
    }

    private static void syncContext(ClientLevel level, Object player) {
        if (level != lastLevel || player != lastPlayer) {
            FROZEN_ENTITIES.clear();
            lastLevel = level;
            lastPlayer = player;
        }
    }
}
