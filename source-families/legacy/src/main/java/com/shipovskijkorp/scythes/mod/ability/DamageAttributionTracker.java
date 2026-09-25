package com.shipovskijkorp.scythes.mod.ability;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

public final class DamageAttributionTracker {
    private DamageAttributionTracker() {}

    private static final Map<UUID, TrackedSource> BLEEDING = new HashMap<>();
    private static final Map<UUID, TrackedSource> WITHERING = new HashMap<>();
    private static final Map<UUID, TrackedSource> FREEZING = new HashMap<>();
    private static final long CLEANUP_INTERVAL_TICKS = 20L;
    private static long nextCleanupAt = Long.MIN_VALUE;

    public static void recordBleeding(LivingEntity target, ServerPlayerEntity owner, int ticks) { record(BLEEDING, target, owner, ticks); }
    public static void recordWithering(LivingEntity target, ServerPlayerEntity owner, int ticks) { record(WITHERING, target, owner, ticks); }
    public static void recordFreezing(LivingEntity target, ServerPlayerEntity owner, int ticks) { record(FREEZING, target, owner, ticks); }

    @Nullable public static ServerPlayerEntity getBleedingOwner(LivingEntity target) { return getOwner(BLEEDING, target); }
    @Nullable public static ServerPlayerEntity getWitheringOwner(LivingEntity target) { return getOwner(WITHERING, target); }
    @Nullable public static ServerPlayerEntity getFreezingOwner(LivingEntity target) { return getOwner(FREEZING, target); }

    public static void clear(LivingEntity target) {
        UUID id = target.getUuid();
        BLEEDING.remove(id); WITHERING.remove(id); FREEZING.remove(id);
    }

    public static void cleanup(MinecraftServer server) {
        long now = server.getOverworld().getTime();
        if (now < nextCleanupAt) return;
        nextCleanupAt = now + CLEANUP_INTERVAL_TICKS;
        cleanup(BLEEDING, now); cleanup(WITHERING, now); cleanup(FREEZING, now);
    }

    public static void clearAll() { BLEEDING.clear(); WITHERING.clear(); FREEZING.clear(); nextCleanupAt = Long.MIN_VALUE; }

    private static void record(Map<UUID, TrackedSource> map, LivingEntity target, ServerPlayerEntity owner, int ticks) {
        if (target.getWorld().isClient || ticks <= 0 || target.getUuid().equals(owner.getUuid())) return;
        map.put(target.getUuid(), new TrackedSource(owner.getUuid(), target.getWorld().getTime() + ticks + 20L));
    }

    @Nullable
    private static ServerPlayerEntity getOwner(Map<UUID, TrackedSource> map, LivingEntity target) {
        TrackedSource tracked = getTracked(map, target);
        if (tracked == null) return null;
        MinecraftServer server = target.getServer();
        if (server == null) return null;
        ServerPlayerEntity owner = server.getPlayerManager().getPlayer(tracked.ownerUuid);
        return owner == null || !owner.isAlive() || owner.isSpectator() ? null : owner;
    }

    @Nullable
    private static TrackedSource getTracked(Map<UUID, TrackedSource> map, LivingEntity target) {
        TrackedSource tracked = map.get(target.getUuid());
        if (tracked == null) return null;
        if (target.getWorld().getTime() > tracked.expiresAt) {
            map.remove(target.getUuid());
            return null;
        }
        return tracked;
    }

    private static void cleanup(Map<UUID, TrackedSource> map, long now) {
        map.entrySet().removeIf(entry -> now > entry.getValue().expiresAt);
    }

    private record TrackedSource(UUID ownerUuid, long expiresAt) {}
}
