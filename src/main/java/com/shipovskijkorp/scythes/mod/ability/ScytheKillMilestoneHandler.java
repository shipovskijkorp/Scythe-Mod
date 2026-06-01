package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.item.BloodScytheItem;
import com.shipovskijkorp.scythes.mod.item.PlagueScytheItem;
import com.shipovskijkorp.scythes.mod.item.WitheringScytheItem;
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

            // Bloody milestones remain PvP-oriented. Plague/Withering are PvE-capable.
            if (kind == MilestoneAdvancements.Kind.BLOODY && !(victim instanceof ServerPlayerEntity)) return;

            if ((kind == MilestoneAdvancements.Kind.PLAGUE || kind == MilestoneAdvancements.Kind.WITHERING)
                    && !ScytheTargeting.canHit(killer, victim)) {
                return;
            }

            if (victim instanceof ServerPlayerEntity playerVictim && killer.isTeammate(playerVictim)) return;

            MilestoneAdvancements.record(killer, kind);
        });
    }

    private static MilestoneAdvancements.Kind detectKind(ServerPlayerEntity killer, LivingEntity victim) {
        Item main = killer.getMainHandStack().getItem();
        Item off = killer.getOffHandStack().getItem();

        // приоритет mainhand -> offhand
        if (main instanceof BloodScytheItem) return MilestoneAdvancements.Kind.BLOODY;
        if (main instanceof PlagueScytheItem) return MilestoneAdvancements.Kind.PLAGUE;
        if (main instanceof WitheringScytheItem) return MilestoneAdvancements.Kind.WITHERING;

        if (off instanceof BloodScytheItem) return MilestoneAdvancements.Kind.BLOODY;
        if (off instanceof PlagueScytheItem) return MilestoneAdvancements.Kind.PLAGUE;
        if (off instanceof WitheringScytheItem) return MilestoneAdvancements.Kind.WITHERING;

        MilestoneAdvancements.Kind trackedKind = DamageAttributionTracker.getKillKind(killer, victim);
        if (trackedKind != null) return trackedKind;

        // fallback: если убийство было активкой, а косы в руках уже нет
        if (PlagueScytheTracker.isActive(killer)) return MilestoneAdvancements.Kind.PLAGUE;
        if (WitheringScytheTracker.isActive(killer)) return MilestoneAdvancements.Kind.WITHERING;

        return null;
    }
}
