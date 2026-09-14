package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.item.BloodScytheItem;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.minecraft.server.network.ServerPlayerEntity;

public class BloodHarvestKillHandler {

    public static void register() {
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, entity, killedEntity, damageSource) -> {

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
        });
    }
}
