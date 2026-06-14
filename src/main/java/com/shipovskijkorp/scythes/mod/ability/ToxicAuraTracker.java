package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.network.ToxicAuraHudS2CPacket;
import com.shipovskijkorp.scythes.mod.util.ScytheCombatUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ToxicAuraTracker {

    private ToxicAuraTracker() {
    }

    public static final int DURATION_TICKS = 20 * 10;
    public static final double RADIUS = 5.0D;
    public static final int POISON_TICKS = 20 * 10;
    public static final int POISON_AMPLIFIER = 1;
    public static final double PURE_DAMAGE_CHANCE = 0.20D;
    public static final float PURE_DAMAGE = 1.0F;
    public static final int ARMOR_DAMAGE_PER_TICK = 1;
    public static final double NAUSEA_CHANCE = 0.15D;
    public static final int NAUSEA_TICKS = 20 * 10;

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
            ToxicAuraHudS2CPacket.sendStop(player);
            return;
        }

        applyAuraTick(player);

        ticksLeft--;
        if (ticksLeft <= 0) {
            ACTIVE.remove(player.getUuid());
            ToxicAuraHudS2CPacket.sendStop(player);
        } else {
            ACTIVE.put(player.getUuid(), ticksLeft);
        }
    }

    private static void applyAuraTick(ServerPlayerEntity player) {
        Box box = player.getBoundingBox().expand(RADIUS);
        DamageSource damageSource = player.getDamageSources().indirectMagic(player, player);

        List<LivingEntity> targets = player.getWorld().getEntitiesByClass(
                LivingEntity.class,
                box,
                target -> !ScytheCombatUtil.isInvalidHostileTarget(player, target)
        );

        for (LivingEntity target : targets) {
            ScytheCombatUtil.refreshStatus(target, StatusEffects.POISON, POISON_TICKS, POISON_AMPLIFIER);
            ScytheAdvancementTracker.recordToxicPoison(player, target, POISON_TICKS);
            ScytheCombatUtil.damageArmorSet(target, ARMOR_DAMAGE_PER_TICK);

            if (player.getRandom().nextDouble() < PURE_DAMAGE_CHANCE) {
                target.damage(damageSource, PURE_DAMAGE);
            }

            if (player.getRandom().nextDouble() < NAUSEA_CHANCE) {
                ScytheCombatUtil.refreshStatus(target, StatusEffects.NAUSEA, NAUSEA_TICKS, 0);
            }
        }
    }
}
