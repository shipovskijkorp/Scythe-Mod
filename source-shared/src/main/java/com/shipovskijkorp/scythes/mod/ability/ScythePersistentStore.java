package com.shipovskijkorp.scythes.mod.ability;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Small world-local persistence store for gameplay state that must survive reconnects and server restarts.
 * Minecraft-facing adapters only supply the world data path; the file format and state semantics stay shared.
 */
public final class ScythePersistentStore {
    private ScythePersistentStore() {}

    private static final String FILE_NAME = "scythes_runtime_state.tsv";

    private static final Map<UUID, Map<String, Long>> COOLDOWNS = new HashMap<>();
    private static final Set<UUID> PENDING_BLOOD_FAILURES = new HashSet<>();
    private static final Map<UUID, Set<UUID>> ACTIVE_MINIONS = new HashMap<>();
    private static final Map<UUID, List<DormantMinion>> DORMANT_MINIONS = new HashMap<>();
    private static final Map<UUID, Integer> PENDING_SOUL_REFUNDS = new HashMap<>();
    private static final Map<UUID, Set<UUID>> GOLDEN_MARK_OWNERS_BY_TARGET = new HashMap<>();
    private static final Map<String, Map<Long, CropAcceleration>> ACCELERATED_CROPS = new HashMap<>();

    private static Path loadedFile;
    private static boolean loaded;

    public static synchronized int remainingCooldown(Path worldRoot, UUID owner, String skill, long now) {
        ensureLoaded(worldRoot);
        Map<String, Long> skills = COOLDOWNS.get(owner);
        if (skills == null) return 0;
        Long readyAt = skills.get(skill);
        if (readyAt == null) return 0;
        if (readyAt <= now) {
            skills.remove(skill);
            if (skills.isEmpty()) COOLDOWNS.remove(owner);
            save();
            return 0;
        }
        long remaining = readyAt - now;
        return remaining > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) remaining;
    }

    public static synchronized void startCooldown(Path worldRoot, UUID owner, String skill, long now, int ticks) {
        ensureLoaded(worldRoot);
        if (ticks <= 0) {
            Map<String, Long> skills = COOLDOWNS.get(owner);
            if (skills != null) {
                skills.remove(skill);
                if (skills.isEmpty()) COOLDOWNS.remove(owner);
                save();
            }
            return;
        }
        long readyAt = now > Long.MAX_VALUE - ticks ? Long.MAX_VALUE : now + ticks;
        COOLDOWNS.computeIfAbsent(owner, ignored -> new HashMap<>()).put(skill, readyAt);
        save();
    }

    public static synchronized void queueBloodFailure(Path worldRoot, UUID owner) {
        ensureLoaded(worldRoot);
        if (PENDING_BLOOD_FAILURES.add(owner)) save();
    }

    public static synchronized boolean consumeBloodFailure(Path worldRoot, UUID owner) {
        ensureLoaded(worldRoot);
        boolean removed = PENDING_BLOOD_FAILURES.remove(owner);
        if (removed) save();
        return removed;
    }

    public static synchronized void registerMinion(Path worldRoot, UUID owner, UUID minion) {
        ensureLoaded(worldRoot);
        if (ACTIVE_MINIONS.computeIfAbsent(owner, ignored -> new HashSet<>()).add(minion)) save();
    }

    public static synchronized void unregisterMinion(Path worldRoot, UUID owner, UUID minion) {
        ensureLoaded(worldRoot);
        Set<UUID> ids = ACTIVE_MINIONS.get(owner);
        if (ids == null || !ids.remove(minion)) return;
        if (ids.isEmpty()) ACTIVE_MINIONS.remove(owner);
        save();
    }

    public static synchronized int minionCount(Path worldRoot, UUID owner) {
        ensureLoaded(worldRoot);
        int active = ACTIVE_MINIONS.getOrDefault(owner, Set.of()).size();
        int dormant = DORMANT_MINIONS.getOrDefault(owner, List.of()).size();
        return active + dormant;
    }

    public static synchronized Set<UUID> activeMinions(Path worldRoot, UUID owner) {
        ensureLoaded(worldRoot);
        return Set.copyOf(ACTIVE_MINIONS.getOrDefault(owner, Set.of()));
    }

    public static synchronized void storeDormantMinion(Path worldRoot, UUID owner, UUID minion, float health, int lifeTicks) {
        ensureLoaded(worldRoot);
        Set<UUID> active = ACTIVE_MINIONS.get(owner);
        if (active != null) {
            active.remove(minion);
            if (active.isEmpty()) ACTIVE_MINIONS.remove(owner);
        }
        List<DormantMinion> dormant = DORMANT_MINIONS.computeIfAbsent(owner, ignored -> new ArrayList<>());
        dormant.add(new DormantMinion(Math.max(0.0F, health), Math.max(0, lifeTicks)));
        save();
    }

    public static synchronized List<DormantMinion> takeDormantMinions(Path worldRoot, UUID owner) {
        ensureLoaded(worldRoot);
        List<DormantMinion> dormant = DORMANT_MINIONS.remove(owner);
        if (dormant == null || dormant.isEmpty()) return List.of();
        save();
        return List.copyOf(dormant);
    }

    public static synchronized List<DormantMinion> dormantMinions(Path worldRoot, UUID owner) {
        ensureLoaded(worldRoot);
        return List.copyOf(DORMANT_MINIONS.getOrDefault(owner, List.of()));
    }

    public static synchronized boolean removeDormantMinion(Path worldRoot, UUID owner, DormantMinion snapshot) {
        ensureLoaded(worldRoot);
        List<DormantMinion> dormant = DORMANT_MINIONS.get(owner);
        if (dormant == null || !dormant.remove(snapshot)) return false;
        if (dormant.isEmpty()) DORMANT_MINIONS.remove(owner);
        save();
        return true;
    }

    public static synchronized void restoreDormantMinions(Path worldRoot, UUID owner, List<DormantMinion> minions) {
        if (minions == null || minions.isEmpty()) return;
        ensureLoaded(worldRoot);
        DORMANT_MINIONS.computeIfAbsent(owner, ignored -> new ArrayList<>()).addAll(minions);
        save();
    }

    public static synchronized void addPendingSoulRefund(Path worldRoot, UUID owner, int amount) {
        if (amount <= 0) return;
        ensureLoaded(worldRoot);
        PENDING_SOUL_REFUNDS.merge(owner, amount, Integer::sum);
        save();
    }

    public static synchronized int pendingSoulRefund(Path worldRoot, UUID owner) {
        ensureLoaded(worldRoot);
        return PENDING_SOUL_REFUNDS.getOrDefault(owner, 0);
    }

    public static synchronized void consumePendingSoulRefund(Path worldRoot, UUID owner, int amount) {
        if (amount <= 0) return;
        ensureLoaded(worldRoot);
        int current = PENDING_SOUL_REFUNDS.getOrDefault(owner, 0);
        if (current <= amount) PENDING_SOUL_REFUNDS.remove(owner);
        else PENDING_SOUL_REFUNDS.put(owner, current - amount);
        save();
    }


    public static synchronized boolean markGoldenTarget(Path worldRoot, UUID target, UUID owner) {
        ensureLoaded(worldRoot);
        boolean added = GOLDEN_MARK_OWNERS_BY_TARGET.computeIfAbsent(target, ignored -> new HashSet<>()).add(owner);
        if (added) save();
        return added;
    }

    public static synchronized boolean isGoldenTargetMarkedBy(Path worldRoot, UUID target, UUID owner) {
        ensureLoaded(worldRoot);
        Set<UUID> owners = GOLDEN_MARK_OWNERS_BY_TARGET.get(target);
        return owners != null && owners.contains(owner);
    }

    public static synchronized void clearGoldenTarget(Path worldRoot, UUID target) {
        ensureLoaded(worldRoot);
        if (GOLDEN_MARK_OWNERS_BY_TARGET.remove(target) != null) save();
    }


    public static synchronized boolean isCropAccelerated(
            Path worldRoot, String dimension, long position, String cropKey, int currentAge) {
        ensureLoaded(worldRoot);
        Map<Long, CropAcceleration> positions = ACCELERATED_CROPS.get(dimension);
        if (positions == null) return false;
        CropAcceleration stored = positions.get(position);
        if (stored == null) return false;

        // A different crop, an unsupported block, or an age rollback means the
        // original plant disappeared and a new one now occupies this position.
        // Natural crop growth never decreases age, so this also catches a same-type
        // crop being broken by automation/water and replanted without our break hook.
        if (!stored.cropKey().equals(cropKey) || currentAge < stored.minimumAge()) {
            positions.remove(position);
            if (positions.isEmpty()) ACCELERATED_CROPS.remove(dimension);
            save();
            return false;
        }
        return true;
    }

    public static synchronized void markCropAccelerated(
            Path worldRoot, String dimension, long position, String cropKey, int minimumAge) {
        ensureLoaded(worldRoot);
        CropAcceleration value = new CropAcceleration(cropKey, Math.max(0, minimumAge));
        CropAcceleration previous = ACCELERATED_CROPS
                .computeIfAbsent(dimension, ignored -> new HashMap<>())
                .put(position, value);
        if (!Objects.equals(previous, value)) save();
    }

    public static synchronized void clearCropAcceleration(Path worldRoot, String dimension, long position) {
        ensureLoaded(worldRoot);
        Map<Long, CropAcceleration> positions = ACCELERATED_CROPS.get(dimension);
        if (positions == null || positions.remove(position) == null) return;
        if (positions.isEmpty()) ACCELERATED_CROPS.remove(dimension);
        save();
    }

    public static synchronized Map<UUID, Set<UUID>> goldenMarks(Path worldRoot) {
        ensureLoaded(worldRoot);
        Map<UUID, Set<UUID>> copy = new HashMap<>();
        for (Map.Entry<UUID, Set<UUID>> entry : GOLDEN_MARK_OWNERS_BY_TARGET.entrySet()) {
            copy.put(entry.getKey(), Set.copyOf(entry.getValue()));
        }
        return Map.copyOf(copy);
    }

    /** Drops only the process cache. Persisted gameplay state remains on disk. */
    public static synchronized void unload() {
        COOLDOWNS.clear();
        PENDING_BLOOD_FAILURES.clear();
        ACTIVE_MINIONS.clear();
        DORMANT_MINIONS.clear();
        PENDING_SOUL_REFUNDS.clear();
        GOLDEN_MARK_OWNERS_BY_TARGET.clear();
        ACCELERATED_CROPS.clear();
        loadedFile = null;
        loaded = false;
    }

    private static void ensureLoaded(Path worldRoot) {
        Path file = worldRoot.resolve("data").resolve(FILE_NAME).toAbsolutePath().normalize();
        if (!Objects.equals(file, loadedFile)) {
            unload();
            loadedFile = file;
        }
        if (loaded) return;
        loaded = true;
        load();
    }

    private static void load() {
        if (loadedFile == null || !Files.isRegularFile(loadedFile)) return;
        try (BufferedReader reader = Files.newBufferedReader(loadedFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] parts = line.split("\\t");
                try {
                    switch (parts[0]) {
                        case "C" -> {
                            if (parts.length != 4) continue;
                            UUID owner = UUID.fromString(parts[1]);
                            COOLDOWNS.computeIfAbsent(owner, ignored -> new HashMap<>())
                                    .put(parts[2], Long.parseLong(parts[3]));
                        }
                        case "B" -> {
                            if (parts.length != 2) continue;
                            PENDING_BLOOD_FAILURES.add(UUID.fromString(parts[1]));
                        }
                        case "A" -> {
                            if (parts.length != 3) continue;
                            UUID owner = UUID.fromString(parts[1]);
                            ACTIVE_MINIONS.computeIfAbsent(owner, ignored -> new HashSet<>())
                                    .add(UUID.fromString(parts[2]));
                        }
                        case "D" -> {
                            if (parts.length != 4) continue;
                            UUID owner = UUID.fromString(parts[1]);
                            DORMANT_MINIONS.computeIfAbsent(owner, ignored -> new ArrayList<>())
                                    .add(new DormantMinion(Float.parseFloat(parts[2]), Integer.parseInt(parts[3])));
                        }
                        case "R" -> {
                            if (parts.length != 3) continue;
                            PENDING_SOUL_REFUNDS.put(UUID.fromString(parts[1]), Math.max(0, Integer.parseInt(parts[2])));
                        }
                        case "G" -> {
                            if (parts.length != 3) continue;
                            UUID target = UUID.fromString(parts[1]);
                            GOLDEN_MARK_OWNERS_BY_TARGET.computeIfAbsent(target, ignored -> new HashSet<>())
                                    .add(UUID.fromString(parts[2]));
                        }
                        case "F" -> {
                            if (parts.length != 4 && parts.length != 5) continue;
                            int minimumAge = parts.length == 5 ? Math.max(0, Integer.parseInt(parts[4])) : 0;
                            ACCELERATED_CROPS.computeIfAbsent(parts[1], ignored -> new HashMap<>())
                                    .put(Long.parseLong(parts[2]), new CropAcceleration(parts[3], minimumAge));
                        }
                        default -> {
                            // Forward-compatible: ignore records from a newer format.
                        }
                    }
                } catch (IllegalArgumentException ignored) {
                    // Ignore a broken record without discarding the rest of the state file.
                }
            }
        } catch (IOException ignored) {
            // Keep an empty in-memory state if persistence is temporarily unavailable.
        }
    }

    private static void save() {
        if (loadedFile == null) return;
        try {
            Files.createDirectories(loadedFile.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(loadedFile, StandardCharsets.UTF_8)) {
                writer.write("# Scythe Mod persistent runtime state v1");
                writer.newLine();
                for (Map.Entry<UUID, Map<String, Long>> owner : COOLDOWNS.entrySet()) {
                    for (Map.Entry<String, Long> skill : owner.getValue().entrySet()) {
                        writer.write("C\t" + owner.getKey() + "\t" + skill.getKey() + "\t" + skill.getValue());
                        writer.newLine();
                    }
                }
                for (UUID owner : PENDING_BLOOD_FAILURES) {
                    writer.write("B\t" + owner);
                    writer.newLine();
                }
                for (Map.Entry<UUID, Set<UUID>> owner : ACTIVE_MINIONS.entrySet()) {
                    for (UUID minion : owner.getValue()) {
                        writer.write("A\t" + owner.getKey() + "\t" + minion);
                        writer.newLine();
                    }
                }
                for (Map.Entry<UUID, List<DormantMinion>> owner : DORMANT_MINIONS.entrySet()) {
                    for (DormantMinion minion : owner.getValue()) {
                        writer.write("D\t" + owner.getKey() + "\t" + minion.health() + "\t" + minion.lifeTicks());
                        writer.newLine();
                    }
                }
                for (Map.Entry<UUID, Integer> owner : PENDING_SOUL_REFUNDS.entrySet()) {
                    if (owner.getValue() <= 0) continue;
                    writer.write("R\t" + owner.getKey() + "\t" + owner.getValue());
                    writer.newLine();
                }
                for (Map.Entry<UUID, Set<UUID>> target : GOLDEN_MARK_OWNERS_BY_TARGET.entrySet()) {
                    for (UUID owner : target.getValue()) {
                        writer.write("G\t" + target.getKey() + "\t" + owner);
                        writer.newLine();
                    }
                }
                for (Map.Entry<String, Map<Long, CropAcceleration>> dimension : ACCELERATED_CROPS.entrySet()) {
                    for (Map.Entry<Long, CropAcceleration> crop : dimension.getValue().entrySet()) {
                        writer.write("F\t" + dimension.getKey() + "\t" + crop.getKey() + "\t"
                                + crop.getValue().cropKey() + "\t" + crop.getValue().minimumAge());
                        writer.newLine();
                    }
                }
            }
        } catch (IOException ignored) {
            // Gameplay keeps working in memory; a later mutation will retry the save.
        }
    }

    public record DormantMinion(float health, int lifeTicks) {}
    public record CropAcceleration(String cropKey, int minimumAge) {}
}
