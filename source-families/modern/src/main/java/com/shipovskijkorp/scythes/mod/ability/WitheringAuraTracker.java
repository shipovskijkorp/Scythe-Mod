package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.entity.WitheringMinionEntity;
import com.shipovskijkorp.scythes.mod.platform.HudSync;
import com.shipovskijkorp.scythes.mod.platform.HudTransport;
import com.shipovskijkorp.scythes.mod.util.ScytheCombatUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
//? if >=1.21.11 {
import net.minecraft.server.world.ServerWorld;
//? } else {
//? }
import net.minecraft.util.math.Box;

public final class WitheringAuraTracker {

    private WitheringAuraTracker() {
    }

    private static final Map<UUID, Integer> ACTIVE = new HashMap<>();

    public static int start(ServerPlayerEntity player) {
        ACTIVE.put(player.getUuid(), ScytheBalance.WitheringAura.DURATION_TICKS);
        return ScytheBalance.WitheringAura.DURATION_TICKS;
    }

    public static void clear(ServerPlayerEntity player) {
        ACTIVE.remove(player.getUuid());
    }

    public static boolean isActive(ServerPlayerEntity player) {
        return ACTIVE.containsKey(player.getUuid());
    }

    public static void tick(ServerPlayerEntity player) {
        Integer ticksLeft = ACTIVE.get(player.getUuid());
        if (ticksLeft == null) return;

        if (!player.isAlive() || player.isSpectator()) {
            ACTIVE.remove(player.getUuid());
            HudSync.stop(player, HudTransport.Timer.WITHERING_AURA);
            return;
        }

        applyAuraTick(player);

        ticksLeft--;
        if (ticksLeft <= 0) {
            ACTIVE.remove(player.getUuid());
            HudSync.stop(player, HudTransport.Timer.WITHERING_AURA);
        } else {
            ACTIVE.put(player.getUuid(), ticksLeft);
        }
    }

    private static void applyAuraTick(ServerPlayerEntity player) {
//? if >=1.21.11 {
        ServerWorld world = (ServerWorld) player.getEntityWorld();
//? } else {
//? }
        Box witherBox = player.getBoundingBox().expand(ScytheBalance.WitheringAura.WITHER_RADIUS);
//? if >=1.21.11 {
        List<LivingEntity> targets = world.getEntitiesByClass(
//? } else {
        List<LivingEntity> targets = player.getWorld().getEntitiesByClass(
//? }
                LivingEntity.class,
                witherBox,
                target -> !(target instanceof WitheringMinionEntity minion && minion.isOwner(player))
                        && !ScytheCombatUtil.isInvalidHostileTarget(player, target)
        );

        for (LivingEntity target : targets) {
            ScytheCombatUtil.refreshStatus(target, StatusEffects.WITHER, ScytheBalance.WitheringAura.WITHER_TICKS, ScytheBalance.WitheringAura.WITHER_AMPLIFIER);
            DamageAttributionTracker.recordWithering(target, player, ScytheBalance.WitheringAura.WITHER_TICKS);
        }

        Box buffBox = player.getBoundingBox().expand(ScytheBalance.WitheringAura.MINION_BUFF_RADIUS);
//? if >=1.21.11 {
        List<WitheringMinionEntity> minions = world.getEntitiesByClass(
//? } else {
        List<WitheringMinionEntity> minions = player.getWorld().getEntitiesByClass(
//? }
                WitheringMinionEntity.class,
                buffBox,
                minion -> minion.isAlive() && minion.isOwner(player)
        );

        for (WitheringMinionEntity minion : minions) {
            ScytheCombatUtil.refreshStatus(minion, StatusEffects.STRENGTH, ScytheBalance.WitheringAura.MINION_BUFF_TICKS, ScytheBalance.WitheringAura.MINION_BUFF_AMPLIFIER);
            ScytheCombatUtil.refreshStatus(minion, StatusEffects.SPEED, ScytheBalance.WitheringAura.MINION_BUFF_TICKS, ScytheBalance.WitheringAura.MINION_BUFF_AMPLIFIER);
        }
    }
}
