package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.util.ScytheCombatUtil;
import com.shipovskijkorp.scythes.mod.item.GoldenScytheItem;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

public final class GoldenLootMarkTracker {
    private GoldenLootMarkTracker() {}

    private static final int PARTICLE_INTERVAL_TICKS = 10;
    private static final DustParticleOptions GOLD_MARK_PARTICLE = new DustParticleOptions(0xFFCC14, 1.35F);
    private static long nextParticleTick;

    public static boolean mark(LivingEntity target, ServerPlayer owner) {
        if (!(target.level() instanceof ServerLevel) || !GoldenScytheItem.isValidGoldenTarget(owner, target)) return false;
        MinecraftServer server = owner.level().getServer();
        if (server == null) return false;
        boolean added = ScythePersistentStore.markGoldenTarget(root(server), target.getUUID(), owner.getUUID());
        if (added) spawnMarkParticles(target, 12);
        return added;
    }

    public static int markAround(ServerPlayer owner, double radius) {
        AABB box = owner.getBoundingBox().inflate(radius);
        int marked = 0;
        for (LivingEntity target : owner.level().getEntitiesOfClass(
                LivingEntity.class,
                box,
                target -> GoldenScytheItem.isValidGoldenTarget(owner, target)
                        && ScytheCombatUtil.isWithinRadius(owner, target, radius)
        )) {
            if (mark(target, owner)) marked++;
        }
        return marked;
    }

    public static boolean isMarkedBy(LivingEntity target, ServerPlayer owner) {
        MinecraftServer server = owner.level().getServer();
        return server != null && ScythePersistentStore.isGoldenTargetMarkedBy(root(server), target.getUUID(), owner.getUUID());
    }

    public static void clear(LivingEntity target) {
        MinecraftServer server = target.level().getServer();
        if (server != null) ScythePersistentStore.clearGoldenTarget(root(server), target.getUUID());
    }

    public static void clearOwner(ServerPlayer owner) {
        // Marks intentionally survive owner disconnects.
    }

    public static void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        if (now < nextParticleTick) return;
        nextParticleTick = now + PARTICLE_INTERVAL_TICKS;

        Path root = root(server);
        for (Map.Entry<UUID, Set<UUID>> entry : ScythePersistentStore.goldenMarks(root).entrySet()) {
            LivingEntity target = findTarget(server, entry.getKey());
            if (target == null) continue;
            if (!target.isAlive() || entry.getValue().isEmpty()) {
                ScythePersistentStore.clearGoldenTarget(root, entry.getKey());
                continue;
            }
            spawnMarkParticles(target, 2);
        }
    }

    private static LivingEntity findTarget(MinecraftServer server, UUID targetId) {
        for (ServerLevel world : server.getAllLevels()) {
            Entity entity = world.getEntity(targetId);
            if (entity instanceof LivingEntity living) return living;
        }
        return null;
    }

    private static Path root(MinecraftServer server) { return ScytheRuntimeState.worldRoot(server); }

    private static void spawnMarkParticles(LivingEntity target, int count) {
        if (!(target.level() instanceof ServerLevel world)) return;
        world.sendParticles(GOLD_MARK_PARTICLE, target.getX(), target.getY() + target.getBbHeight() + 0.35D, target.getZ(), count, 0.22D, 0.08D, 0.22D, 0.01D);
    }
}
