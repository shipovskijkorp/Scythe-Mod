package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.entity.WitheringMinionEntity;
import com.shipovskijkorp.scythes.mod.network.WitheringAuraHudS2CPacket;
import com.shipovskijkorp.scythes.mod.util.ScytheCombatUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;

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

    public static int start(ServerPlayer player) {
        ACTIVE.put(player.getUUID(), DURATION_TICKS);
        return DURATION_TICKS;
    }

    public static void clear(ServerPlayer player) {
        ACTIVE.remove(player.getUUID());
    }

    public static boolean isActive(ServerPlayer player) {
        return ACTIVE.containsKey(player.getUUID());
    }

    public static void tick(ServerPlayer player) {
        Integer ticksLeft = ACTIVE.get(player.getUUID());
        if (ticksLeft == null) return;

        if (!player.isAlive() || player.isSpectator()) {
            ACTIVE.remove(player.getUUID());
            WitheringAuraHudS2CPacket.sendStop(player);
            return;
        }

        applyAuraTick(player);

        ticksLeft--;
        if (ticksLeft <= 0) {
            ACTIVE.remove(player.getUUID());
            WitheringAuraHudS2CPacket.sendStop(player);
        } else {
            ACTIVE.put(player.getUUID(), ticksLeft);
        }
    }

    private static void applyAuraTick(ServerPlayer player) {
        ServerLevel world = (ServerLevel) player.level();
        AABB witherBox = player.getBoundingBox().inflate(WITHER_RADIUS);
        List<LivingEntity> targets = world.getEntitiesOfClass(
                LivingEntity.class,
                witherBox,
                target -> !(target instanceof WitheringMinionEntity minion && minion.isOwner(player))
                        && !ScytheCombatUtil.isInvalidHostileTarget(player, target)
        );

        for (LivingEntity target : targets) {
            ScytheCombatUtil.refreshStatus(target, MobEffects.WITHER, WITHER_TICKS, WITHER_AMPLIFIER);
            DamageAttributionTracker.recordWithering(target, player, WITHER_TICKS);
        }

        AABB buffBox = player.getBoundingBox().inflate(MINION_BUFF_RADIUS);
        List<WitheringMinionEntity> minions = world.getEntitiesOfClass(
                WitheringMinionEntity.class,
                buffBox,
                minion -> minion.isAlive() && minion.isOwner(player)
        );

        for (WitheringMinionEntity minion : minions) {
            ScytheCombatUtil.refreshStatus(minion, MobEffects.STRENGTH, MINION_BUFF_TICKS, MINION_BUFF_AMPLIFIER);
            ScytheCombatUtil.refreshStatus(minion, MobEffects.SPEED, MINION_BUFF_TICKS, MINION_BUFF_AMPLIFIER);
        }
    }
}
