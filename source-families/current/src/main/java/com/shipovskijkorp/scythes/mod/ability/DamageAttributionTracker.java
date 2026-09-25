package com.shipovskijkorp.scythes.mod.ability;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public final class DamageAttributionTracker {
    private DamageAttributionTracker() {}

    private static final Map<UUID, TrackedSource> BLEEDING = new HashMap<>();
    private static final Map<UUID, TrackedSource> WITHERING = new HashMap<>();
    private static final Map<UUID, TrackedSource> FREEZING = new HashMap<>();
    private static final long CLEANUP_INTERVAL_TICKS = 20L;
    private static long nextCleanupAt = Long.MIN_VALUE;

    public static void recordBleeding(LivingEntity target, ServerPlayer owner, int ticks) { record(BLEEDING, target, owner, ticks); }
    public static void recordWithering(LivingEntity target, ServerPlayer owner, int ticks) { record(WITHERING, target, owner, ticks); }
    public static void recordFreezing(LivingEntity target, ServerPlayer owner, int ticks) { record(FREEZING, target, owner, ticks); }

    @Nullable public static ServerPlayer getBleedingOwner(LivingEntity target) { return getOwner(BLEEDING, target); }
    @Nullable public static ServerPlayer getWitheringOwner(LivingEntity target) { return getOwner(WITHERING, target); }
    @Nullable public static ServerPlayer getFreezingOwner(LivingEntity target) { return getOwner(FREEZING, target); }

    public static void clear(LivingEntity target) {
        UUID id = target.getUUID();
        BLEEDING.remove(id); WITHERING.remove(id); FREEZING.remove(id);
    }

    public static void cleanup(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        if (now < nextCleanupAt) return;
        nextCleanupAt = now + CLEANUP_INTERVAL_TICKS;
        cleanup(BLEEDING, now); cleanup(WITHERING, now); cleanup(FREEZING, now);
    }

    public static void clearAll() { BLEEDING.clear(); WITHERING.clear(); FREEZING.clear(); nextCleanupAt = Long.MIN_VALUE; }

    private static void record(Map<UUID, TrackedSource> map, LivingEntity target, ServerPlayer owner, int ticks) {
        if (ticks <= 0 || target.getUUID().equals(owner.getUUID())) return;
        map.put(target.getUUID(), new TrackedSource(owner.getUUID(), owner.level().getGameTime() + ticks + 20L));
    }

    @Nullable
    private static ServerPlayer getOwner(Map<UUID, TrackedSource> map, LivingEntity target) {
        TrackedSource tracked = getTracked(map, target);
        if (tracked == null) return null;
        if (!(target.level() instanceof net.minecraft.server.level.ServerLevel level)) return null;
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(tracked.ownerUuid);
        return owner == null || !owner.isAlive() || owner.isSpectator() ? null : owner;
    }

    @Nullable
    private static TrackedSource getTracked(Map<UUID, TrackedSource> map, LivingEntity target) {
        TrackedSource tracked = map.get(target.getUUID());
        if (tracked == null) return null;
        if (target.level().getGameTime() > tracked.expiresAt) {
            map.remove(target.getUUID());
            return null;
        }
        return tracked;
    }

    private static void cleanup(Map<UUID, TrackedSource> map, long now) {
        map.entrySet().removeIf(entry -> now > entry.getValue().expiresAt);
    }

    private record TrackedSource(UUID ownerUuid, long expiresAt) {}
}
