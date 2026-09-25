package com.shipovskijkorp.scythes.mod.util;

import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import java.util.List;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class FireTargeting {
    private FireTargeting() {}

    public static boolean isValidTarget(PlayerEntity owner, LivingEntity target) {
        if (ScytheCombatUtil.isProtectedWitheringMinion(owner, target)) return false;
        if (target == owner || !target.isAlive() || target.isSpectator()) return false;
        if (owner.isTeammate(target)) return false;
        return target instanceof PlayerEntity || target instanceof HostileEntity;
    }

    public static LivingEntity findLockTarget(PlayerEntity player) {
        double range = ScytheBalance.Fire.FIREBALL_LOCK_RANGE;
        Box box = player.getBoundingBox().expand(range);
        List<LivingEntity> candidates = player.getWorld().getEntitiesByClass(
                LivingEntity.class,
                box,
                target -> isValidTarget(player, target) && target.squaredDistanceTo(player) <= range * range
        );

        Vec3d eye = player.getCameraPosVec(1.0F);
        Vec3d look = player.getRotationVec(1.0F).normalize();
        LivingEntity best = null;
        int bestPriority = -1;
        double bestDot = ScytheBalance.Fire.FIREBALL_LOCK_MIN_DOT;
        double bestDistance = Double.MAX_VALUE;

        for (LivingEntity target : candidates) {
            if (!player.canSee(target)) continue;
            Vec3d point = target.getPos().add(0.0D, target.getHeight() * 0.5D, 0.0D);
            Vec3d delta = point.subtract(eye);
            double distance = delta.lengthSquared();
            if (distance <= 1.0E-6D) continue;
            double dot = look.dotProduct(delta.normalize());
            if (dot < ScytheBalance.Fire.FIREBALL_LOCK_MIN_DOT) continue;

            int priority = target instanceof PlayerEntity ? 1 : 0;
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
