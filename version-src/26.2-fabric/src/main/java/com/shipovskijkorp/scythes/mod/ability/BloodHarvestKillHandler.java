package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.item.BloodScytheItem;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.minecraft.server.level.ServerPlayer;

public class BloodHarvestKillHandler {

    public static void register() {
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, entity, killedEntity, damageSource) -> {

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
        });
    }
}
