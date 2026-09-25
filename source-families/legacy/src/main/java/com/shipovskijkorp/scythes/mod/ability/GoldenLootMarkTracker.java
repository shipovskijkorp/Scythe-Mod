package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.util.ScytheCombatUtil;
import com.shipovskijkorp.scythes.mod.item.GoldenScytheItem;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import org.joml.Vector3f;

public final class GoldenLootMarkTracker {
    private GoldenLootMarkTracker() {}

    private static final int PARTICLE_INTERVAL_TICKS = 10;
    private static final DustParticleEffect GOLD_MARK_PARTICLE = new DustParticleEffect(new Vector3f(1.0F, 0.78F, 0.08F), 1.35F);
    private static long nextParticleTick;

    public static boolean mark(LivingEntity target, ServerPlayerEntity owner) {
        if (target.getWorld().isClient || !GoldenScytheItem.isValidGoldenTarget(owner, target)) return false;
        MinecraftServer server = owner.getServerWorld().getServer();
        if (server == null) return false;
        boolean added = ScythePersistentStore.markGoldenTarget(root(server), target.getUuid(), owner.getUuid());
        if (added) spawnMarkParticles(target, 12);
        return added;
    }

    public static int markAround(ServerPlayerEntity owner, double radius) {
        Box box = owner.getBoundingBox().expand(radius);
        int marked = 0;
        for (LivingEntity target : owner.getWorld().getEntitiesByClass(
                LivingEntity.class,
                box,
                target -> GoldenScytheItem.isValidGoldenTarget(owner, target)
                        && ScytheCombatUtil.isWithinRadius(owner, target, radius)
        )) {
            if (mark(target, owner)) marked++;
        }
        return marked;
    }

    public static boolean isMarkedBy(LivingEntity target, ServerPlayerEntity owner) {
        MinecraftServer server = owner.getServerWorld().getServer();
        return server != null && ScythePersistentStore.isGoldenTargetMarkedBy(root(server), target.getUuid(), owner.getUuid());
    }

    public static void clear(LivingEntity target) {
        MinecraftServer server = target.getWorld().getServer();
        if (server != null) ScythePersistentStore.clearGoldenTarget(root(server), target.getUuid());
    }

    public static void clearOwner(ServerPlayerEntity owner) {
        // Marks are world-persistent gameplay state and intentionally survive owner disconnects.
    }

    public static void tick(MinecraftServer server) {
        ServerWorld overworld = server.getOverworld();
        if (overworld == null) return;
        long now = overworld.getTime();
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
        for (ServerWorld world : server.getWorlds()) {
            if (world.getEntity(targetId) instanceof LivingEntity living) return living;
        }
        return null;
    }

    private static Path root(MinecraftServer server) {
        return ScytheRuntimeState.worldRoot(server);
    }

    private static void spawnMarkParticles(LivingEntity target, int count) {
        if (!(target.getWorld() instanceof ServerWorld world)) return;
        world.spawnParticles(GOLD_MARK_PARTICLE, target.getX(), target.getY() + target.getHeight() + 0.35D, target.getZ(), count, 0.22D, 0.08D, 0.22D, 0.01D);
    }
}
