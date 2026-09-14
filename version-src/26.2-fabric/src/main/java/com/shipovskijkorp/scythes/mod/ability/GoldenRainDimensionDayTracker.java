package com.shipovskijkorp.scythes.mod.ability;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class GoldenRainDimensionDayTracker {

    private GoldenRainDimensionDayTracker() {
    }

    private static final long TICKS_PER_DAY = 24000L;
    private static final String FILE_NAME = "scythes_golden_rain_progress.tsv";
    private static final Map<UUID, List<ActivationRecord>> RECORDS_BY_PLAYER = new HashMap<>();

    private static Path loadedPath;
    private static boolean loaded;

    public static boolean recordActivation(ServerPlayer player) {
        Path savePath = resolveSavePath(player);
        ensureLoaded(savePath);

        String dimension = player.level().dimension().toString();
        MinecraftServer server = player.level().getServer();
        long dayTime = server == null
                ? player.level().getGameTime()
                : server.overworld().dimensionTypeRegistration().value().defaultClock()
                        .map(clock -> server.clockManager().getTotalTicks(clock))
                        .orElse(server.overworld().getGameTime());
        long day = Math.floorDiv(dayTime, TICKS_PER_DAY);

        List<ActivationRecord> records = RECORDS_BY_PLAYER.computeIfAbsent(player.getUUID(), ignored -> new ArrayList<>());
        ActivationRecord record = new ActivationRecord(dimension, day);
        if (!records.contains(record)) {
            records.add(record);
            save(savePath);
        }

        return hasThreeDimensionDays(records);
    }

    public static void clear(ServerPlayer player) {
        // Golden Rain progress is stored in the world data file and must survive reconnects/server restarts.
        // This method intentionally does not remove saved progress.
    }

    private static void ensureLoaded(Path savePath) {
        if (!Objects.equals(loadedPath, savePath)) {
            RECORDS_BY_PLAYER.clear();
            loadedPath = savePath;
            loaded = false;
        }

        if (loaded) return;
        loaded = true;
        load(savePath);
    }

    private static void load(Path savePath) {
        if (!Files.isRegularFile(savePath)) return;

        try (BufferedReader reader = Files.newBufferedReader(savePath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\t", 3);
                if (parts.length != 3) continue;

                try {
                    UUID playerId = UUID.fromString(parts[0]);
                    String dimension = unescape(parts[1]);
                    long day = Long.parseLong(parts[2]);
                    RECORDS_BY_PLAYER
                            .computeIfAbsent(playerId, ignored -> new ArrayList<>())
                            .add(new ActivationRecord(dimension, day));
                } catch (IllegalArgumentException ignored) {
                    // Ignore broken lines so one corrupted record does not break the whole advancement tracker.
                }
            }
        } catch (IOException ignored) {
            // If the file cannot be read, the tracker still works for the current session.
        }
    }

    private static void save(Path savePath) {
        try {
            Path parent = savePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (BufferedWriter writer = Files.newBufferedWriter(savePath, StandardCharsets.UTF_8)) {
                for (Map.Entry<UUID, List<ActivationRecord>> entry : RECORDS_BY_PLAYER.entrySet()) {
                    for (ActivationRecord record : entry.getValue()) {
                        writer.write(entry.getKey().toString());
                        writer.write('\t');
                        writer.write(escape(record.dimension()));
                        writer.write('\t');
                        writer.write(Long.toString(record.day()));
                        writer.newLine();
                    }
                }
            }
        } catch (IOException ignored) {
            // Advancement progress remains in memory if disk persistence is temporarily unavailable.
        }
    }

    private static Path resolveSavePath(ServerPlayer player) {
        try {
            Object server = invokeNoArg(player.level(), "getServer");
            Path worldRoot = resolveWorldRoot(server);
            if (worldRoot != null) {
                return worldRoot.resolve("data").resolve(FILE_NAME);
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Fall through to a process-local path. This keeps the code compatible with nearby mapping/API shifts.
        }

        return Path.of(FILE_NAME);
    }

    private static Path resolveWorldRoot(Object server) throws ReflectiveOperationException {
        Class<?> levelResourceClass = Class.forName("net.minecraft.world.level.storage.LevelResource");
        Field rootField = levelResourceClass.getDeclaredField("ROOT");
        rootField.setAccessible(true);
        Object rootResource = rootField.get(null);

        for (Method method : server.getClass().getMethods()) {
            if (!method.getName().equals("getWorldPath")) continue;
            if (method.getParameterCount() != 1) continue;
            if (!method.getParameterTypes()[0].isAssignableFrom(levelResourceClass)
                    && !levelResourceClass.isAssignableFrom(method.getParameterTypes()[0])) continue;

            Object path = method.invoke(server, rootResource);
            if (path instanceof Path worldPath) {
                return worldPath;
            }
        }

        return null;
    }

    private static Object invokeNoArg(Object target, String methodName) throws ReflectiveOperationException {
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Method method = type.getDeclaredMethod(methodName);
                method.setAccessible(true);
                return method.invoke(target);
            } catch (NoSuchMethodException ignored) {
                // Try parent class.
            }
        }

        throw new NoSuchMethodException(methodName);
    }

    private static boolean hasThreeDimensionDays(List<ActivationRecord> records) {
        for (int i = 0; i < records.size(); i++) {
            ActivationRecord first = records.get(i);
            for (int j = i + 1; j < records.size(); j++) {
                ActivationRecord second = records.get(j);
                if (!areDifferentDimensionDays(first, second)) continue;

                for (int k = j + 1; k < records.size(); k++) {
                    ActivationRecord third = records.get(k);
                    if (areDifferentDimensionDays(first, third) && areDifferentDimensionDays(second, third)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static boolean areDifferentDimensionDays(ActivationRecord first, ActivationRecord second) {
        return !first.dimension().equals(second.dimension()) && first.day() != second.day();
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n").replace("\r", "\\r");
    }

    private static String unescape(String value) {
        StringBuilder result = new StringBuilder(value.length());
        boolean escaped = false;

        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (!escaped) {
                if (ch == '\\') {
                    escaped = true;
                } else {
                    result.append(ch);
                }
                continue;
            }

            switch (ch) {
                case 't' -> result.append('\t');
                case 'n' -> result.append('\n');
                case 'r' -> result.append('\r');
                default -> result.append(ch);
            }
            escaped = false;
        }

        if (escaped) {
            result.append('\\');
        }
        return result.toString();
    }

    private record ActivationRecord(String dimension, long day) {
    }
}
