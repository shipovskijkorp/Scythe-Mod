package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

public final class FireLaunchTracker {
    private static final class Pending {
        final LivingEntity target;
        int ticks;
        Pending(LivingEntity target) { this.target = target; this.ticks = ScytheBalance.FireBurst.LAUNCH_DELAY_TICKS; }
    }
    private static final List<Pending> PENDING = new ArrayList<>();
    private FireLaunchTracker() {}

    public static void schedule(LivingEntity target) {
        PENDING.removeIf(p -> p.target == target);
        PENDING.add(new Pending(target));
    }

    public static void tick() {
        Iterator<Pending> it = PENDING.iterator();
        while (it.hasNext()) {
            Pending pending = it.next();
            LivingEntity target = pending.target;
            if (!target.isAlive() || !(target.level() instanceof ServerLevel level)) {
                it.remove();
                continue;
            }
            level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(), 5,
                    target.getBbWidth() * 0.3D, target.getBbHeight() * 0.3D, target.getBbWidth() * 0.3D, 0.02D);
            if (--pending.ticks <= 0) {
                target.push(0.0D, ScytheBalance.FireBurst.LAUNCH_VELOCITY_Y - target.getDeltaMovement().y, 0.0D);
                it.remove();
            }
        }
    }

    public static void clearAll() { PENDING.clear(); }
}
