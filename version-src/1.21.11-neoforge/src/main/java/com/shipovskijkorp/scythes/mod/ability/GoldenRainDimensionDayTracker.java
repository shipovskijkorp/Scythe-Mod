package com.shipovskijkorp.scythes.mod.ability;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.shipovskijkorp.scythes.mod.ScytheMod;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;

public final class GoldenRainDimensionDayTracker {

    private GoldenRainDimensionDayTracker() {
    }

    private static final String STATE_ID = ScytheMod.MOD_ID + "_golden_rain_dimension_days";
    private static final long TICKS_PER_DAY = 24000L;
    private static final int MAX_ENTRIES_PER_PLAYER = 128;

    public static boolean recordActivation(ServerPlayerEntity player) {
        if (!(player.getEntityWorld() instanceof ServerWorld playerWorld)) return false;

        MinecraftServer server = playerWorld.getServer();
        ServerWorld overworld = server.getOverworld();
        if (overworld == null) return false;

        GoldenRainState state = overworld.getPersistentStateManager().getOrCreate(GoldenRainState.TYPE);

        String dimension = playerWorld.getRegistryKey().getValue().toString();
        long day = Math.floorDiv(overworld.getTimeOfDay(), TICKS_PER_DAY);
        return state.record(player.getUuid(), dimension, day);
    }

    private static final class GoldenRainState extends PersistentState {
        private static final Codec<ActivationRecord> ACTIVATION_RECORD_CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("dimension").forGetter(ActivationRecord::dimension),
                Codec.LONG.fieldOf("day").forGetter(ActivationRecord::day)
        ).apply(instance, ActivationRecord::new));

        private static final Codec<PlayerRecord> PLAYER_RECORD_CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("uuid").forGetter(PlayerRecord::uuid),
                ACTIVATION_RECORD_CODEC.listOf().optionalFieldOf("entries", List.of()).forGetter(PlayerRecord::entries)
        ).apply(instance, PlayerRecord::new));

        private static final Codec<GoldenRainState> CODEC = PLAYER_RECORD_CODEC.listOf()
                .optionalFieldOf("players", List.of())
                .codec()
                .xmap(GoldenRainState::fromPlayerRecords, GoldenRainState::toPlayerRecords);

        private static final PersistentStateType<GoldenRainState> TYPE = new PersistentStateType<>(
                STATE_ID,
                GoldenRainState::new,
                CODEC,
                null
        );

        private final Map<UUID, List<ActivationRecord>> recordsByPlayer = new HashMap<>();

        private GoldenRainState() {
        }

        private static GoldenRainState fromPlayerRecords(List<PlayerRecord> playerRecords) {
            GoldenRainState state = new GoldenRainState();

            for (PlayerRecord playerRecord : playerRecords) {
                if (playerRecord.uuid().isEmpty()) continue;

                UUID playerUuid;
                try {
                    playerUuid = UUID.fromString(playerRecord.uuid());
                } catch (IllegalArgumentException ignored) {
                    continue;
                }

                List<ActivationRecord> records = new ArrayList<>();
                for (ActivationRecord record : playerRecord.entries()) {
                    if (!record.dimension().isEmpty()) {
                        addUnique(records, record);
                    }
                }

                if (!records.isEmpty()) {
                    state.recordsByPlayer.put(playerUuid, records);
                }
            }

            return state;
        }

        private List<PlayerRecord> toPlayerRecords() {
            List<PlayerRecord> players = new ArrayList<>();
            for (Map.Entry<UUID, List<ActivationRecord>> entry : recordsByPlayer.entrySet()) {
                players.add(new PlayerRecord(entry.getKey().toString(), List.copyOf(entry.getValue())));
            }
            return players;
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

    private record PlayerRecord(String uuid, List<ActivationRecord> entries) {
    }

    private record ActivationRecord(String dimension, long day) {
    }
}
