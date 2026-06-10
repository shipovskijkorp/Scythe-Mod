package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.item.BloodScytheItem;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;

public final class ScytheKillMilestoneHandler {

    private ScytheKillMilestoneHandler() {}

    public static void register() {
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, entity, killedEntity) -> {

            if (!(entity instanceof ServerPlayerEntity killer)) return;
            LivingEntity victim = killedEntity;

            MilestoneAdvancements.Kind kind = detectKind(killer, victim);
            if (kind == null) return;

            // Bloody milestones remain PvP-oriented.
            if (kind == MilestoneAdvancements.Kind.BLOODY && !(victim instanceof ServerPlayerEntity)) return;

            if (victim instanceof ServerPlayerEntity playerVictim && killer.isTeammate(playerVictim)) return;

            MilestoneAdvancements.record(killer, kind);
        });
    }

    private static MilestoneAdvancements.Kind detectKind(ServerPlayerEntity killer, LivingEntity victim) {
        Item main = killer.getMainHandStack().getItem();
        Item off = killer.getOffHandStack().getItem();

        // приоритет mainhand -> offhand
        if (main instanceof BloodScytheItem) return MilestoneAdvancements.Kind.BLOODY;
        if (off instanceof BloodScytheItem) return MilestoneAdvancements.Kind.BLOODY;

        return DamageAttributionTracker.getKillKind(killer, victim);
    }
}
