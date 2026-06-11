package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.entity.WitheringMinionEntity;
import com.shipovskijkorp.scythes.mod.network.WitheringAuraHudS2CPacket;
import com.shipovskijkorp.scythes.mod.util.ScytheCombatUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class WitheringAuraTracker {

    private WitheringAuraTracker() {
    }

    public static final int DURATION_TICKS = 20 * 10;
    public static final double WITHER_RADIUS = 5.0D;
    public static final int WITHER_TICKS = 20 * 10;
    public static final int WITHER_AMPLIFIER = 1;
    public static final double MINION_BUFF_RADIUS = 10.0D;
    public static final int MINION_BUFF_TICKS = 20 * 3;
    public static final int MINION_BUFF_AMPLIFIER = 1;

    private static final Map<UUID, Integer> ACTIVE = new HashMap<>();

    public static int start(ServerPlayerEntity player) {
        ACTIVE.put(player.getUuid(), DURATION_TICKS);
        return DURATION_TICKS;
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
            WitheringAuraHudS2CPacket.sendStop(player);
            return;
        }

        applyAuraTick(player);

        ticksLeft--;
        if (ticksLeft <= 0) {
            ACTIVE.remove(player.getUuid());
            WitheringAuraHudS2CPacket.sendStop(player);
        } else {
            ACTIVE.put(player.getUuid(), ticksLeft);
        }
    }

    private static void applyAuraTick(ServerPlayerEntity player) {
        Box witherBox = player.getBoundingBox().expand(WITHER_RADIUS);
        List<LivingEntity> targets = player.getWorld().getEntitiesByClass(
                LivingEntity.class,
                witherBox,
                target -> !(target instanceof WitheringMinionEntity minion && minion.isOwner(player))
                        && !ScytheCombatUtil.isInvalidHostileTarget(player, target)
        );

        for (LivingEntity target : targets) {
            ScytheCombatUtil.refreshStatus(target, StatusEffects.WITHER, WITHER_TICKS, WITHER_AMPLIFIER);
            DamageAttributionTracker.recordWithering(target, player, WITHER_TICKS);
        }

        Box buffBox = player.getBoundingBox().expand(MINION_BUFF_RADIUS);
        List<WitheringMinionEntity> minions = player.getWorld().getEntitiesByClass(
                WitheringMinionEntity.class,
                buffBox,
                minion -> minion.isAlive() && minion.isOwner(player)
        );

        for (WitheringMinionEntity minion : minions) {
            ScytheCombatUtil.refreshStatus(minion, StatusEffects.STRENGTH, MINION_BUFF_TICKS, MINION_BUFF_AMPLIFIER);
            ScytheCombatUtil.refreshStatus(minion, StatusEffects.SPEED, MINION_BUFF_TICKS, MINION_BUFF_AMPLIFIER);
        }
    }
}
