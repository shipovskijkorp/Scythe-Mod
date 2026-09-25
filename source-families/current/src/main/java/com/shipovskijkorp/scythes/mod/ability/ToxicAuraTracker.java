package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.item.ToxicScytheItem;
import com.shipovskijkorp.scythes.mod.platform.HudSync;
import com.shipovskijkorp.scythes.mod.platform.HudTransport;
import com.shipovskijkorp.scythes.mod.util.ScytheCombatUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

public final class ToxicAuraTracker {

    private ToxicAuraTracker() {
    }

//? if >=26.2 {
    private static final Map<UUID, AuraState> ACTIVE = new HashMap<>();
//? } else {
    private static final Map<UUID, ActiveAura> ACTIVE = new HashMap<>();
//? }

    public static int start(ServerPlayer player, int acidityLevel) {
//? if >=26.2 {
        ACTIVE.put(player.getUUID(), new AuraState(ScytheBalance.ToxicAura.DURATION_TICKS, Math.max(0, acidityLevel)));
//? } else {
        ACTIVE.put(player.getUUID(), new ActiveAura(ScytheBalance.ToxicAura.DURATION_TICKS, Math.max(0, Math.min(3, acidityLevel))));
//? }
        return ScytheBalance.ToxicAura.DURATION_TICKS;
    }

    public static void clear(ServerPlayer player) {
        ACTIVE.remove(player.getUUID());
    }

    public static boolean isActive(ServerPlayer player) {
        return ACTIVE.containsKey(player.getUUID());
    }

    public static void tick(ServerPlayer player) {
//? if >=26.2 {
        AuraState state = ACTIVE.get(player.getUUID());
        if (state == null) return;
//? } else {
        ActiveAura aura = ACTIVE.get(player.getUUID());
        if (aura == null) return;
//? }

        if (!player.isAlive() || player.isSpectator()) {
            ACTIVE.remove(player.getUUID());
            HudSync.stop(player, HudTransport.Timer.TOXIC_AURA);
            return;
        }

//? if >=26.2 {
        applyAuraTick(player, state.acidityLevel());
//? } else {
        applyAuraTick(player, aura.acidityLevel());
//? }

//? if >=26.2 {
        int ticksLeft = state.ticksLeft() - 1;
//? } else {
        int ticksLeft = aura.ticksLeft() - 1;
//? }
        if (ticksLeft <= 0) {
            ACTIVE.remove(player.getUUID());
            HudSync.stop(player, HudTransport.Timer.TOXIC_AURA);
        } else {
//? if >=26.2 {
            ACTIVE.put(player.getUUID(), new AuraState(ticksLeft, state.acidityLevel()));
//? } else {
            ACTIVE.put(player.getUUID(), new ActiveAura(ticksLeft, aura.acidityLevel()));
//? }
        }
    }

    private static void applyAuraTick(ServerPlayer player, int acidityLevel) {
        ServerLevel world = (ServerLevel) player.level();
        AABB box = player.getBoundingBox().inflate(ScytheBalance.ToxicAura.RADIUS);
        DamageSource damageSource = player.damageSources().indirectMagic(player, player);

        List<LivingEntity> targets = world.getEntitiesOfClass(
                LivingEntity.class,
                box,
                target -> ScytheCombatUtil.isValidCombatTargetWithin(player, target, ScytheBalance.ToxicAura.RADIUS)
        );

        for (LivingEntity target : targets) {
            ScytheCombatUtil.refreshStatus(target, MobEffects.POISON, ScytheBalance.ToxicAura.POISON_TICKS, ScytheBalance.ToxicAura.POISON_AMPLIFIER);
            ScytheAdvancementTracker.recordToxicPoison(player, target, ScytheBalance.ToxicAura.POISON_TICKS);
            ScytheCombatUtil.damageArmorSet(target, ToxicScytheItem.applyAcidityBonus(player.getRandom(), ScytheBalance.ToxicAura.ARMOR_DAMAGE_PER_TICK, acidityLevel));

            if (player.getRandom().nextDouble() < ScytheBalance.ToxicAura.PURE_DAMAGE_CHANCE) {
                target.hurtServer(world, damageSource, ScytheBalance.ToxicAura.PURE_DAMAGE);
            }

            if (player.getRandom().nextDouble() < ScytheBalance.ToxicAura.NAUSEA_CHANCE) {
                ScytheCombatUtil.refreshStatus(target, MobEffects.NAUSEA, ScytheBalance.ToxicAura.NAUSEA_TICKS, ScytheBalance.ToxicAura.NAUSEA_AMPLIFIER);
            }
        }
    }

//? if >=26.2 {
    private record AuraState(int ticksLeft, int acidityLevel) {
//? } else {
    private record ActiveAura(int ticksLeft, int acidityLevel) {
//? }
    }
}
