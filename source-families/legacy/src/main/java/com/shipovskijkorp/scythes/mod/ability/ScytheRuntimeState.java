package com.shipovskijkorp.scythes.mod.ability;

import java.nio.file.Path;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

/** Minecraft-version adapter for the shared persistent gameplay state. */
public final class ScytheRuntimeState {
    private ScytheRuntimeState() {}

    public static Path worldRoot(MinecraftServer server) {
        return server.getSavePath(WorldSavePath.ROOT);
    }
}
