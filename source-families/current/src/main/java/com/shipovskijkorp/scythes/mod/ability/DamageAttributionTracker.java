package com.shipovskijkorp.scythes.mod.ability;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class DamageAttributionTracker {

    private DamageAttributionTracker() {
    }

    private static final Map<UUID, TrackedSource> BLEEDING = new HashMap<>();
    private static final Map<UUID, TrackedSource> WITHERING = new HashMap<>();

    public static void recordBleeding(LivingEntity target, ServerPlayer owner, int ticks) {
        record(BLEEDING, target, owner, ticks);
    }

    public static void recordWithering(LivingEntity target, ServerPlayer owner, int ticks) {
        record(WITHERING, target, owner, ticks);
    }

    @Nullable
    public static ServerPlayer getBleedingOwner(LivingEntity target) {
        return getOwner(BLEEDING, target);
    }

    @Nullable
    public static ServerPlayer getWitheringOwner(LivingEntity target) {
        return getOwner(WITHERING, target);
    }

    public static void clear(LivingEntity target) {
        BLEEDING.remove(target.getUUID());
        WITHERING.remove(target.getUUID());
    }

    private static void record(Map<UUID, TrackedSource> map,
                               LivingEntity target,
                               ServerPlayer owner,
                               int ticks) {
        if (ticks <= 0) return;
        if (target.getUUID().equals(owner.getUUID())) return;

        long expiresAt = owner.level().getGameTime() + ticks + 20L;
        map.put(target.getUUID(), new TrackedSource(owner.getUUID(), owner, expiresAt));
    }

    @Nullable
    private static ServerPlayer getOwner(Map<UUID, TrackedSource> map, LivingEntity target) {
        TrackedSource tracked = getTracked(map, target);
        if (tracked == null) return null;

        ServerPlayer owner = tracked.owner;
        if (owner == null || !owner.isAlive() || owner.isSpectator()) {
            return null;
        }

        return owner;
    }

    @Nullable
    private static TrackedSource getTracked(Map<UUID, TrackedSource> map, LivingEntity target) {
        TrackedSource tracked = map.get(target.getUUID());
        if (tracked == null) return null;

        ServerPlayer owner = tracked.owner;
        if (owner == null || owner.level().getGameTime() > tracked.expiresAt) {
            map.remove(target.getUUID());
            return null;
        }

        return tracked;
    }

    private record TrackedSource(UUID ownerUuid, ServerPlayer owner, long expiresAt) {
    }
}
