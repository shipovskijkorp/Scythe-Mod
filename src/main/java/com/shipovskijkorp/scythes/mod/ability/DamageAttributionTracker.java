package com.shipovskijkorp.scythes.mod.ability;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class DamageAttributionTracker {

    private DamageAttributionTracker() {
    }

    private static final Map<UUID, TrackedSource> BLEEDING = new HashMap<>();
    private static final Map<UUID, TrackedSource> WITHERING = new HashMap<>();

    public static void recordBleeding(LivingEntity target, ServerPlayerEntity owner, int ticks) {
        record(BLEEDING, target, owner, ticks, MilestoneAdvancements.Kind.BLOODY);
    }

    public static void recordWithering(LivingEntity target, ServerPlayerEntity owner, int ticks) {
        record(WITHERING, target, owner, ticks, MilestoneAdvancements.Kind.WITHERING);
    }

    @Nullable
    public static ServerPlayerEntity getBleedingOwner(LivingEntity target) {
        return getOwner(BLEEDING, target);
    }

    @Nullable
    public static ServerPlayerEntity getWitheringOwner(LivingEntity target) {
        return getOwner(WITHERING, target);
    }

    @Nullable
    public static MilestoneAdvancements.Kind getKillKind(ServerPlayerEntity owner, LivingEntity target) {
        TrackedSource bleeding = getTracked(BLEEDING, target);
        if (bleeding != null && bleeding.ownerUuid.equals(owner.getUuid())) {
            return bleeding.kind;
        }

        TrackedSource withering = getTracked(WITHERING, target);
        if (withering != null && withering.ownerUuid.equals(owner.getUuid())) {
            return withering.kind;
        }

        return null;
    }

    public static void clear(LivingEntity target) {
        UUID targetUuid = target.getUuid();
        BLEEDING.remove(targetUuid);
        WITHERING.remove(targetUuid);
    }

    private static void record(Map<UUID, TrackedSource> map,
                               LivingEntity target,
                               ServerPlayerEntity owner,
                               int ticks,
                               MilestoneAdvancements.Kind kind) {
        if (target.getWorld().isClient) return;
        if (ticks <= 0) return;
        if (target.getUuid().equals(owner.getUuid())) return;

        long expiresAt = target.getWorld().getTime() + ticks + 20L;
        map.put(target.getUuid(), new TrackedSource(owner.getUuid(), expiresAt, kind));
    }

    @Nullable
    private static ServerPlayerEntity getOwner(Map<UUID, TrackedSource> map, LivingEntity target) {
        TrackedSource tracked = getTracked(map, target);
        if (tracked == null) return null;

        MinecraftServer server = target.getServer();
        if (server == null) return null;

        ServerPlayerEntity owner = server.getPlayerManager().getPlayer(tracked.ownerUuid);
        if (owner == null || !owner.isAlive() || owner.isSpectator()) {
            return null;
        }

        return owner;
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

    private record TrackedSource(UUID ownerUuid, long expiresAt, MilestoneAdvancements.Kind kind) {
    }
}
