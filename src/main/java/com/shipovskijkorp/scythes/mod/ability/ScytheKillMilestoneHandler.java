package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.item.BloodScytheItem;
import com.shipovskijkorp.scythes.mod.item.PlagueScytheItem;
import com.shipovskijkorp.scythes.mod.item.WitheringScytheItem;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;

public final class ScytheKillMilestoneHandler {

    private ScytheKillMilestoneHandler() {}

    public static void register() {
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, entity, killedEntity) -> {

            if (!(entity instanceof ServerPlayerEntity killer)) return;
            if (!(killedEntity instanceof ServerPlayerEntity victim)) return;

            // kill milestones не считаем по тиммейтам (анти-фарм)
            if (killer.isTeammate(victim)) return;

            MilestoneAdvancements.Kind kind = detectKind(killer);
            if (kind == null) return;

            MilestoneAdvancements.record(killer, kind);
        });
    }

    private static MilestoneAdvancements.Kind detectKind(ServerPlayerEntity killer) {
        Item main = killer.getMainHandStack().getItem();
        Item off = killer.getOffHandStack().getItem();

        // приоритет mainhand -> offhand
        if (main instanceof BloodScytheItem) return MilestoneAdvancements.Kind.BLOODY;
        if (main instanceof PlagueScytheItem) return MilestoneAdvancements.Kind.PLAGUE;
        if (main instanceof WitheringScytheItem) return MilestoneAdvancements.Kind.WITHERING;

        if (off instanceof BloodScytheItem) return MilestoneAdvancements.Kind.BLOODY;
        if (off instanceof PlagueScytheItem) return MilestoneAdvancements.Kind.PLAGUE;
        if (off instanceof WitheringScytheItem) return MilestoneAdvancements.Kind.WITHERING;

        // fallback: если убийство было активкой (DoT), а косы в руках нет
        if (PlagueScytheTracker.isActive(killer)) return MilestoneAdvancements.Kind.PLAGUE;
        if (WitheringScytheTracker.isActive(killer)) return MilestoneAdvancements.Kind.WITHERING;

        return null;
    }
}
