package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;

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
//? if >=1.21.11 {
            if (!target.isAlive() || !(target.getEntityWorld() instanceof ServerWorld world)) {
//? } else {
            if (!target.isAlive() || !(target.getWorld() instanceof ServerWorld world)) {
//? }
                it.remove();
                continue;
            }
            world.spawnParticles(ParticleTypes.FLAME, target.getX(), target.getY() + target.getHeight() * 0.5D, target.getZ(), 5,
                    target.getWidth() * 0.3D, target.getHeight() * 0.3D, target.getWidth() * 0.3D, 0.02D);
            if (--pending.ticks <= 0) {
                target.addVelocity(0.0D, ScytheBalance.FireBurst.LAUNCH_VELOCITY_Y - target.getVelocity().y, 0.0D);
                it.remove();
            }
        }
    }

    public static void clearAll() { PENDING.clear(); }
}
