package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.item.GoldenScytheItem;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class GoldenLootMarkTracker {

    private GoldenLootMarkTracker() {
    }

    public static final int MARK_LOOTING_BONUS = 2;
    private static final int PARTICLE_INTERVAL_TICKS = 10;
    private static final int GOLD_MARK_COLOR = 0xFFCC14;
    private static final DustParticleOptions GOLD_MARK_PARTICLE = new DustParticleOptions(GOLD_MARK_COLOR, 1.35F);

    private static final Map<UUID, Set<UUID>> MARK_OWNERS_BY_TARGET = new HashMap<>();
    private static long nextParticleTick = 0L;

    public static boolean mark(LivingEntity target, ServerPlayer owner) {
        if (!(target.level() instanceof ServerLevel)) return false;
        if (!GoldenScytheItem.isValidGoldenTarget(owner, target)) return false;

        Set<UUID> owners = MARK_OWNERS_BY_TARGET.computeIfAbsent(target.getUUID(), ignored -> new HashSet<>());
        boolean added = owners.add(owner.getUUID());
        spawnMarkParticles(target, 12);
        return added;
    }

    public static int markAround(ServerPlayer owner, double radius) {
        AABB box = owner.getBoundingBox().inflate(radius);
        double maxDistanceSquared = radius * radius;

        int marked = 0;
        for (LivingEntity target : owner.level().getEntitiesOfClass(
                LivingEntity.class,
                box,
                target -> GoldenScytheItem.isValidGoldenTarget(owner, target)
                        && target.distanceToSqr(owner) <= maxDistanceSquared
        )) {
            mark(target, owner);
            marked++;
        }
        return marked;
    }

    public static boolean isMarkedBy(LivingEntity target, ServerPlayer owner) {
        Set<UUID> owners = MARK_OWNERS_BY_TARGET.get(target.getUUID());
        return owners != null && owners.contains(owner.getUUID());
    }

    public static void clear(LivingEntity target) {
        MARK_OWNERS_BY_TARGET.remove(target.getUUID());
    }

    public static void clearOwner(ServerPlayer owner) {
        UUID ownerId = owner.getUUID();
        for (Iterator<Set<UUID>> iterator = MARK_OWNERS_BY_TARGET.values().iterator(); iterator.hasNext(); ) {
            Set<UUID> owners = iterator.next();
            owners.remove(ownerId);
            if (owners.isEmpty()) {
                iterator.remove();
            }
        }
    }

    public static void tick(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        long now = overworld.getGameTime();
        if (now < nextParticleTick) return;
        nextParticleTick = now + PARTICLE_INTERVAL_TICKS;

        for (Iterator<Map.Entry<UUID, Set<UUID>>> iterator = MARK_OWNERS_BY_TARGET.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<UUID, Set<UUID>> entry = iterator.next();
            LivingEntity target = findTarget(server, entry.getKey());
            if (target == null) {
                continue;
            }
            if (!target.isAlive() || entry.getValue().isEmpty()) {
                iterator.remove();
                continue;
            }
            spawnMarkParticles(target, 2);
        }
    }

    private static LivingEntity findTarget(MinecraftServer server, UUID targetId) {
        for (ServerLevel world : server.getAllLevels()) {
            Entity entity = world.getEntity(targetId);
            if (entity instanceof LivingEntity living) {
                return living;
            }
        }
        return null;
    }

    private static void spawnMarkParticles(LivingEntity target, int count) {
        if (!(target.level() instanceof ServerLevel world)) return;

        world.sendParticles(
                GOLD_MARK_PARTICLE,
                target.getX(),
                target.getY() + target.getBbHeight() + 0.35D,
                target.getZ(),
                count,
                0.22D,
                0.08D,
                0.22D,
                0.01D
        );
    }
}
