package com.shipovskijkorp.scythes.mod.util;

import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class FireTargeting {
    private FireTargeting() {}

    public static boolean isValidTarget(Player owner, LivingEntity target) {
        if (ScytheCombatUtil.isProtectedWitheringMinion(owner, target)) return false;
        if (target == owner || !target.isAlive() || target.isSpectator()) return false;
        if (owner.isAlliedTo(target)) return false;
        return target instanceof Player || target instanceof Monster;
    }

    public static LivingEntity findLockTarget(Player player) {
        double range = ScytheBalance.Fire.FIREBALL_LOCK_RANGE;
        AABB box = player.getBoundingBox().inflate(range);
        List<LivingEntity> candidates = player.level().getEntitiesOfClass(
                LivingEntity.class,
                box,
                target -> isValidTarget(player, target) && target.distanceToSqr(player) <= range * range
        );

        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getLookAngle().normalize();
        LivingEntity best = null;
        int bestPriority = -1;
        double bestDot = ScytheBalance.Fire.FIREBALL_LOCK_MIN_DOT;
        double bestDistance = Double.MAX_VALUE;

        for (LivingEntity target : candidates) {
            if (!player.hasLineOfSight(target)) continue;
            Vec3 point = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
            Vec3 delta = point.subtract(eye);
            double distance = delta.lengthSqr();
            if (distance <= 1.0E-6D) continue;
            double dot = look.dot(delta.normalize());
            if (dot < ScytheBalance.Fire.FIREBALL_LOCK_MIN_DOT) continue;

            int priority = target instanceof Player ? 1 : 0;
            if (priority > bestPriority
                    || (priority == bestPriority && dot > bestDot + 1.0E-6D)
                    || (priority == bestPriority && Math.abs(dot - bestDot) <= 1.0E-6D && distance < bestDistance)) {
                best = target;
                bestPriority = priority;
                bestDot = dot;
                bestDistance = distance;
            }
        }
        return best;
    }
}
