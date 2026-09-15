package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.item.BloodScytheItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public class BloodHarvestKillHandler {

    /** Called once by the loader after a credited kill, on the server thread. */
    public static void onKill(ServerWorld world, Entity entity, LivingEntity killedEntity) {
        if (!(entity instanceof ServerPlayerEntity killer)) return;
        if (!(killedEntity instanceof ServerPlayerEntity victim)) return;

        // Идеальная жатва не считается по тиммейтам
        if (killer.isTeammate(victim)) return;

        if (!BloodHarvestTracker.isActive(killer)) return;

        boolean holdingScythe =
                killer.getMainHandStack().getItem() instanceof BloodScytheItem
                        || killer.getOffHandStack().getItem() instanceof BloodScytheItem;

        if (!holdingScythe) return;

        BloodHarvestTracker.onKill(killer);
    }
}
