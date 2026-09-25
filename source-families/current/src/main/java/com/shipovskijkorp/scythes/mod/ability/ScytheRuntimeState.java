package com.shipovskijkorp.scythes.mod.ability;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import net.minecraft.server.MinecraftServer;

/** Minecraft-version adapter for the shared persistent gameplay state. */
public final class ScytheRuntimeState {
    private ScytheRuntimeState() {}

    public static Path worldRoot(MinecraftServer server) {
        try {
            Class<?> resourceClass = Class.forName("net.minecraft.world.level.storage.LevelResource");
            Field rootField = resourceClass.getDeclaredField("ROOT");
            rootField.setAccessible(true);
            Object root = rootField.get(null);
            for (Method method : server.getClass().getMethods()) {
                if (!method.getName().equals("getWorldPath") || method.getParameterCount() != 1) continue;
                if (!method.getParameterTypes()[0].isInstance(root)) continue;
                Object result = method.invoke(server, root);
                if (result instanceof Path path) return path;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Fall back below. 26.x changed this API more than once while keeping the level data format stable.
        }
        return Path.of(".").toAbsolutePath().normalize();
    }
}
