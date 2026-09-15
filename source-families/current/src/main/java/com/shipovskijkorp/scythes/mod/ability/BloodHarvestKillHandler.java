package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.item.BloodScytheItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class BloodHarvestKillHandler {

    /** Called once by the loader after a credited kill, on the server thread. */
    public static void onKill(ServerLevel world, Entity entity, LivingEntity killedEntity) {
        if (!(entity instanceof ServerPlayer killer)) return;
        if (!(killedEntity instanceof ServerPlayer victim)) return;

        // Идеальная жатва не считается по тиммейтам
        if (killer.isAlliedTo(victim)) return;

        if (!BloodHarvestTracker.isActive(killer)) return;

        boolean holdingScythe =
                killer.getMainHandItem().getItem() instanceof BloodScytheItem
                        || killer.getOffhandItem().getItem() instanceof BloodScytheItem;

        if (!holdingScythe) return;

        BloodHarvestTracker.onKill(killer);
    }
}
