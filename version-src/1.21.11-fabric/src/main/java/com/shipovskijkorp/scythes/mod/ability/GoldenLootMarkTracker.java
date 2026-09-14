package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.item.GoldenScytheItem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
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
    private static final DustParticleEffect GOLD_MARK_PARTICLE = new DustParticleEffect(0xFFCC14, 1.35F);

    private static final Map<UUID, Set<UUID>> MARK_OWNERS_BY_TARGET = new HashMap<>();
    private static long nextParticleTick = 0L;

    public static boolean mark(LivingEntity target, ServerPlayerEntity owner) {
        if (!(target.getEntityWorld() instanceof ServerWorld)) return false;
        if (!GoldenScytheItem.isValidGoldenTarget(owner, target)) return false;

        Set<UUID> owners = MARK_OWNERS_BY_TARGET.computeIfAbsent(target.getUuid(), ignored -> new HashSet<>());
        boolean added = owners.add(owner.getUuid());
        spawnMarkParticles(target, 12);
        return added;
    }

    public static int markAround(ServerPlayerEntity owner, double radius) {
        Box box = owner.getBoundingBox().expand(radius);
        double maxDistanceSquared = radius * radius;

        int marked = 0;
        for (LivingEntity target : owner.getEntityWorld().getEntitiesByClass(
                LivingEntity.class,
                box,
                target -> GoldenScytheItem.isValidGoldenTarget(owner, target)
                        && target.squaredDistanceTo(owner) <= maxDistanceSquared
        )) {
            mark(target, owner);
            marked++;
        }
        return marked;
    }

    public static boolean isMarkedBy(LivingEntity target, ServerPlayerEntity owner) {
        Set<UUID> owners = MARK_OWNERS_BY_TARGET.get(target.getUuid());
        return owners != null && owners.contains(owner.getUuid());
    }

    public static void clear(LivingEntity target) {
        MARK_OWNERS_BY_TARGET.remove(target.getUuid());
    }

    public static void clearOwner(ServerPlayerEntity owner) {
        UUID ownerId = owner.getUuid();
        for (Iterator<Set<UUID>> iterator = MARK_OWNERS_BY_TARGET.values().iterator(); iterator.hasNext(); ) {
            Set<UUID> owners = iterator.next();
            owners.remove(ownerId);
            if (owners.isEmpty()) {
                iterator.remove();
            }
        }
    }

    public static void tick(MinecraftServer server) {
        ServerWorld overworld = server.getOverworld();
        if (overworld == null) return;

        long now = overworld.getTime();
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
        for (ServerWorld world : server.getWorlds()) {
            if (world.getEntity(targetId) instanceof LivingEntity living) {
                return living;
            }
        }
        return null;
    }

    private static void spawnMarkParticles(LivingEntity target, int count) {
        if (!(target.getEntityWorld() instanceof ServerWorld world)) return;

        world.spawnParticles(
                GOLD_MARK_PARTICLE,
                target.getX(),
                target.getY() + target.getHeight() + 0.35D,
                target.getZ(),
                count,
                0.22D,
                0.08D,
                0.22D,
                0.01D
        );
    }
}
