package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.PersistentState;

public final class GoldenRainDimensionDayTracker {

    private GoldenRainDimensionDayTracker() {
    }

    private static final String STATE_ID = ScytheMod.MOD_ID + "_golden_rain_dimension_days";
    private static final long TICKS_PER_DAY = 24000L;
    private static final int MAX_ENTRIES_PER_PLAYER = 128;

    public static boolean recordActivation(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) return false;

        GoldenRainState state = server.getOverworld().getPersistentStateManager().getOrCreate(
                GoldenRainState.TYPE,
                STATE_ID
        );

        String dimension = player.getWorld().getRegistryKey().getValue().toString();
        long day = Math.floorDiv(server.getOverworld().getTimeOfDay(), TICKS_PER_DAY);
        return state.record(player.getUuid(), dimension, day);
    }

    private static final class GoldenRainState extends PersistentState {
        private static final Type<GoldenRainState> TYPE = new Type<>(
                GoldenRainState::new,
                GoldenRainState::fromNbt,
                null
        );

        private final Map<UUID, List<ActivationRecord>> recordsByPlayer = new HashMap<>();

        private static GoldenRainState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
            GoldenRainState state = new GoldenRainState();
            NbtList players = nbt.getList("Players", NbtElement.COMPOUND_TYPE);

            for (int i = 0; i < players.size(); i++) {
                NbtCompound playerNbt = players.getCompound(i);
                if (!playerNbt.containsUuid("Uuid")) continue;

                UUID playerUuid = playerNbt.getUuid("Uuid");
                NbtList entries = playerNbt.getList("Entries", NbtElement.COMPOUND_TYPE);
                List<ActivationRecord> records = new ArrayList<>();

                for (int j = 0; j < entries.size(); j++) {
                    NbtCompound entryNbt = entries.getCompound(j);
                    String dimension = entryNbt.getString("Dimension");
                    long day = entryNbt.getLong("Day");
                    if (!dimension.isEmpty()) {
                        addUnique(records, new ActivationRecord(dimension, day));
                    }
                }

                if (!records.isEmpty()) {
                    state.recordsByPlayer.put(playerUuid, records);
                }
            }

            return state;
        }

        @Override
        public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
            NbtList players = new NbtList();

            for (Map.Entry<UUID, List<ActivationRecord>> playerEntry : recordsByPlayer.entrySet()) {
                NbtCompound playerNbt = new NbtCompound();
                playerNbt.putUuid("Uuid", playerEntry.getKey());

                NbtList entries = new NbtList();
                for (ActivationRecord record : playerEntry.getValue()) {
                    NbtCompound entryNbt = new NbtCompound();
                    entryNbt.putString("Dimension", record.dimension());
                    entryNbt.putLong("Day", record.day());
                    entries.add(entryNbt);
                }

                playerNbt.put("Entries", entries);
                players.add(playerNbt);
            }

            nbt.put("Players", players);
            return nbt;
        }

        private boolean record(UUID playerUuid, String dimension, long day) {
            List<ActivationRecord> records = recordsByPlayer.computeIfAbsent(playerUuid, ignored -> new ArrayList<>());
            addUnique(records, new ActivationRecord(dimension, day));

            while (records.size() > MAX_ENTRIES_PER_PLAYER) {
                records.remove(0);
            }

            markDirty();
            return hasThreeDimensionDays(records);
        }

        private static void addUnique(List<ActivationRecord> records, ActivationRecord candidate) {
            if (!records.contains(candidate)) {
                records.add(candidate);
            }
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
    }

    private record ActivationRecord(String dimension, long day) {
    }
}
