package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.entity.WitheringMinionEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;

import java.util.List;

public final class WitheringMinionManager {

    private WitheringMinionManager() {
    }

    private static final double SEARCH_RADIUS = 160.0D;

    public static int countMinions(ServerPlayerEntity owner) {
        return findMinions(owner).size();
    }

    public static int dismissMinions(ServerPlayerEntity owner) {
        List<WitheringMinionEntity> minions = findMinions(owner);
        for (WitheringMinionEntity minion : minions) {
            minion.discard();
        }
        return minions.size();
    }

    public static boolean spawnMinion(ServerPlayerEntity owner) {
        ServerWorld world = owner.getServerWorld();
        WitheringMinionEntity minion = new WitheringMinionEntity(ScytheMod.WITHERING_MINION, world);
        minion.initializeForOwner(owner);

        double yaw = Math.toRadians(owner.getYaw());
        double x = owner.getX() - Math.sin(yaw) * 1.6D;
        double y = owner.getY();
        double z = owner.getZ() + Math.cos(yaw) * 1.6D;

        minion.refreshPositionAndAngles(x, y, z, owner.getYaw(), 0.0F);
        return world.spawnEntity(minion);
    }

    public static List<WitheringMinionEntity> findMinions(ServerPlayerEntity owner) {
        Box box = owner.getBoundingBox().expand(SEARCH_RADIUS);
        return owner.getServerWorld().getEntitiesByClass(
                WitheringMinionEntity.class,
                box,
                minion -> minion.isAlive() && minion.isOwner(owner)
        );
    }
}
