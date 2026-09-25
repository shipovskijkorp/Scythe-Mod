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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

public final class WitheringAuraTracker {

    private WitheringAuraTracker() {
    }

    private static final Map<UUID, Integer> ACTIVE = new HashMap<>();

    public static int start(ServerPlayer player) {
        ACTIVE.put(player.getUUID(), ScytheBalance.WitheringAura.DURATION_TICKS);
        return ScytheBalance.WitheringAura.DURATION_TICKS;
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
            HudSync.stop(player, HudTransport.Timer.WITHERING_AURA);
            return;
        }

        applyAuraTick(player);

        ticksLeft--;
        if (ticksLeft <= 0) {
            ACTIVE.remove(player.getUUID());
            HudSync.stop(player, HudTransport.Timer.WITHERING_AURA);
        } else {
            ACTIVE.put(player.getUUID(), ticksLeft);
        }
    }

    private static void applyAuraTick(ServerPlayer player) {
        ServerLevel world = (ServerLevel) player.level();
        AABB witherBox = player.getBoundingBox().inflate(ScytheBalance.WitheringAura.WITHER_RADIUS);
        List<LivingEntity> targets = world.getEntitiesOfClass(
                LivingEntity.class,
                witherBox,
                target -> ScytheCombatUtil.isValidCombatTargetWithin(player, target, ScytheBalance.WitheringAura.WITHER_RADIUS)
        );

        for (LivingEntity target : targets) {
            ScytheCombatUtil.refreshStatus(target, MobEffects.WITHER, ScytheBalance.WitheringAura.WITHER_TICKS, ScytheBalance.WitheringAura.WITHER_AMPLIFIER);
            DamageAttributionTracker.recordWithering(target, player, ScytheBalance.WitheringAura.WITHER_TICKS);
        }

        AABB buffBox = player.getBoundingBox().inflate(ScytheBalance.WitheringAura.MINION_BUFF_RADIUS);
        List<WitheringMinionEntity> minions = world.getEntitiesOfClass(
                WitheringMinionEntity.class,
                buffBox,
                minion -> minion.isAlive() && minion.isOwner(player)
                        && ScytheCombatUtil.isWithinRadius(player, minion, ScytheBalance.WitheringAura.MINION_BUFF_RADIUS)
        );

        for (WitheringMinionEntity minion : minions) {
            ScytheCombatUtil.refreshStatus(minion, MobEffects.STRENGTH, ScytheBalance.WitheringAura.MINION_BUFF_TICKS, ScytheBalance.WitheringAura.MINION_BUFF_AMPLIFIER);
            ScytheCombatUtil.refreshStatus(minion, MobEffects.SPEED, ScytheBalance.WitheringAura.MINION_BUFF_TICKS, ScytheBalance.WitheringAura.MINION_BUFF_AMPLIFIER);
        }
    }
}
