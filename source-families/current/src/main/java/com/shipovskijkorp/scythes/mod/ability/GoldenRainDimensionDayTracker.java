package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class GoldenRainDimensionDayTracker {

    private GoldenRainDimensionDayTracker() {
    }

    private static final long TICKS_PER_DAY = 24000L;
    private static final String PROGRESS_FILE_NAME = "scythes_golden_rain_progress.tsv";

    private static final Map<UUID, Set<ActivationRecord>> RECORDS_BY_PLAYER = new HashMap<>();
    private static Path loadedProgressFile;

    public static boolean recordActivation(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return false;

        Path progressFile = getProgressFile(server);
        ensureLoaded(progressFile);

        String dimension = player.level().dimension().toString();
        long dayTime = server.overworld().dimensionTypeRegistration().value().defaultClock()
                .map(clock -> server.clockManager().getTotalTicks(clock))
                .orElse(server.overworld().getGameTime());
        long day = Math.floorDiv(dayTime, TICKS_PER_DAY);

        Set<ActivationRecord> records = RECORDS_BY_PLAYER.computeIfAbsent(player.getUUID(), ignored -> new HashSet<>());
        boolean changed = records.add(new ActivationRecord(dimension, day));
        if (changed) {
            save(progressFile);
        }

        return hasThreeDimensionDays(records);
    }

    public static void clear(ServerPlayer player) {
        // Golden Rain progress is stored in the world folder and intentionally survives disconnects/restarts.
    }

    private static Path getProgressFile(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("data").resolve(PROGRESS_FILE_NAME);
    }

    private static void ensureLoaded(Path progressFile) {
        if (progressFile.equals(loadedProgressFile)) return;

        RECORDS_BY_PLAYER.clear();
        loadedProgressFile = progressFile;

        if (!Files.isRegularFile(progressFile)) return;

        try {
            for (String line : Files.readAllLines(progressFile, StandardCharsets.UTF_8)) {
                if (line.isBlank() || line.startsWith("#")) continue;

                String[] parts = line.split("\\t", 3);
                if (parts.length != 3) continue;

                UUID playerId = UUID.fromString(parts[0]);
                long day = Long.parseLong(parts[2]);
                RECORDS_BY_PLAYER.computeIfAbsent(playerId, ignored -> new HashSet<>())
                        .add(new ActivationRecord(parts[1], day));
            }
        } catch (IllegalArgumentException | IOException exception) {
            ScytheMod.LOGGER.warn("Failed to load Golden Rain advancement progress from {}", progressFile, exception);
            RECORDS_BY_PLAYER.clear();
        }
    }

    private static void save(Path progressFile) {
        List<String> lines = new ArrayList<>();
        lines.add("# player_uuid\\tdimension\\tday");

        for (Map.Entry<UUID, Set<ActivationRecord>> entry : RECORDS_BY_PLAYER.entrySet()) {
            for (ActivationRecord record : entry.getValue()) {
                lines.add(entry.getKey() + "\t" + record.dimension() + "\t" + record.day());
            }
        }

        try {
            Files.createDirectories(progressFile.getParent());
            Files.write(
                    progressFile,
                    lines,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
        } catch (IOException exception) {
            ScytheMod.LOGGER.warn("Failed to save Golden Rain advancement progress to {}", progressFile, exception);
        }
    }

    private static boolean hasThreeDimensionDays(Set<ActivationRecord> records) {
        List<ActivationRecord> list = new ArrayList<>(records);
        for (int i = 0; i < list.size(); i++) {
            ActivationRecord first = list.get(i);
            for (int j = i + 1; j < list.size(); j++) {
                ActivationRecord second = list.get(j);
                if (!areDifferentDimensionDays(first, second)) continue;

                for (int k = j + 1; k < list.size(); k++) {
                    ActivationRecord third = list.get(k);
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

    private record ActivationRecord(String dimension, long day) {
    }
}
