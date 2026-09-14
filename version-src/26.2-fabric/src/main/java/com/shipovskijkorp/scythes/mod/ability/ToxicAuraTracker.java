package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.network.ToxicAuraHudS2CPacket;
import com.shipovskijkorp.scythes.mod.util.ScytheCombatUtil;
import com.shipovskijkorp.scythes.mod.item.ToxicScytheItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;

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

    private static final Map<UUID, AuraState> ACTIVE = new HashMap<>();

    public static int start(ServerPlayer player, int acidityLevel) {
        ACTIVE.put(player.getUUID(), new AuraState(DURATION_TICKS, Math.max(0, acidityLevel)));
        return DURATION_TICKS;
    }

    public static void clear(ServerPlayer player) {
        ACTIVE.remove(player.getUUID());
    }

    public static boolean isActive(ServerPlayer player) {
        return ACTIVE.containsKey(player.getUUID());
    }

    public static void tick(ServerPlayer player) {
        AuraState state = ACTIVE.get(player.getUUID());
        if (state == null) return;

        if (!player.isAlive() || player.isSpectator()) {
            ACTIVE.remove(player.getUUID());
            ToxicAuraHudS2CPacket.sendStop(player);
            return;
        }

        applyAuraTick(player, state.acidityLevel());

        int ticksLeft = state.ticksLeft() - 1;
        if (ticksLeft <= 0) {
            ACTIVE.remove(player.getUUID());
            ToxicAuraHudS2CPacket.sendStop(player);
        } else {
            ACTIVE.put(player.getUUID(), new AuraState(ticksLeft, state.acidityLevel()));
        }
    }

    private static void applyAuraTick(ServerPlayer player, int acidityLevel) {
        ServerLevel world = (ServerLevel) player.level();
        AABB box = player.getBoundingBox().inflate(RADIUS);
        DamageSource damageSource = player.damageSources().indirectMagic(player, player);

        List<LivingEntity> targets = world.getEntitiesOfClass(
                LivingEntity.class,
                box,
                target -> !ScytheCombatUtil.isInvalidHostileTarget(player, target)
        );

        for (LivingEntity target : targets) {
            ScytheCombatUtil.refreshStatus(target, MobEffects.POISON, POISON_TICKS, POISON_AMPLIFIER);
            ScytheAdvancementTracker.recordToxicPoison(player, target, POISON_TICKS);
            ScytheCombatUtil.damageArmorSet(target, ToxicScytheItem.applyAcidityBonus(player.getRandom(), ARMOR_DAMAGE_PER_TICK, acidityLevel));

            if (player.getRandom().nextDouble() < PURE_DAMAGE_CHANCE) {
                target.hurtServer(world, damageSource, PURE_DAMAGE);
            }

            if (player.getRandom().nextDouble() < NAUSEA_CHANCE) {
                ScytheCombatUtil.refreshStatus(target, MobEffects.NAUSEA, NAUSEA_TICKS, 0);
            }
        }
    }

    private record AuraState(int ticksLeft, int acidityLevel) {
    }
}
