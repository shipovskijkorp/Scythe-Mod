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
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;

public final class ToxicAuraTracker {

    private ToxicAuraTracker() {
    }

    private static final Map<UUID, ToxicAuraState> ACTIVE = new HashMap<>();

    public static int start(ServerPlayerEntity player, ItemStack scytheStack) {
        ACTIVE.put(player.getUuid(), new ToxicAuraState(ScytheBalance.ToxicAura.DURATION_TICKS, ToxicScytheItem.getAcidityLevel(scytheStack)));
        return ScytheBalance.ToxicAura.DURATION_TICKS;
    }

    public static void clear(ServerPlayerEntity player) {
        ACTIVE.remove(player.getUuid());
    }

    public static boolean isActive(ServerPlayerEntity player) {
        return ACTIVE.containsKey(player.getUuid());
    }

    public static void tick(ServerPlayerEntity player) {
        ToxicAuraState state = ACTIVE.get(player.getUuid());
        if (state == null) return;

        if (!player.isAlive() || player.isSpectator()) {
            ACTIVE.remove(player.getUuid());
            HudSync.stop(player, HudTransport.Timer.TOXIC_AURA);
            return;
        }

        applyAuraTick(player, state.acidityLevel);

        state.ticksLeft--;
        if (state.ticksLeft <= 0) {
            ACTIVE.remove(player.getUuid());
            HudSync.stop(player, HudTransport.Timer.TOXIC_AURA);
        }
    }

    private static void applyAuraTick(ServerPlayerEntity player, int acidityLevel) {
        Box box = player.getBoundingBox().expand(ScytheBalance.ToxicAura.RADIUS);
        DamageSource damageSource = player.getDamageSources().indirectMagic(player, player);

        List<LivingEntity> targets = player.getWorld().getEntitiesByClass(
                LivingEntity.class,
                box,
                target -> !ScytheCombatUtil.isInvalidHostileTarget(player, target)
        );

        for (LivingEntity target : targets) {
            ScytheCombatUtil.refreshStatus(target, StatusEffects.POISON, ScytheBalance.ToxicAura.POISON_TICKS, ScytheBalance.ToxicAura.POISON_AMPLIFIER);
            ScytheAdvancementTracker.recordToxicPoison(player, target, ScytheBalance.ToxicAura.POISON_TICKS);
            int armorDamage = ToxicScytheItem.applyAcidityArmorDamageBonus(ScytheBalance.ToxicAura.ARMOR_DAMAGE_PER_TICK, acidityLevel, player.getRandom());
            ScytheCombatUtil.damageArmorSet(target, armorDamage);

            if (player.getRandom().nextDouble() < ScytheBalance.ToxicAura.PURE_DAMAGE_CHANCE) {
                target.damage(damageSource, ScytheBalance.ToxicAura.PURE_DAMAGE);
            }

            if (player.getRandom().nextDouble() < ScytheBalance.ToxicAura.NAUSEA_CHANCE) {
                ScytheCombatUtil.refreshStatus(target, StatusEffects.NAUSEA, ScytheBalance.ToxicAura.NAUSEA_TICKS, ScytheBalance.ToxicAura.NAUSEA_AMPLIFIER);
            }
        }
    }

    private static final class ToxicAuraState {
        private int ticksLeft;
        private final int acidityLevel;

        private ToxicAuraState(int ticksLeft, int acidityLevel) {
            this.ticksLeft = ticksLeft;
            this.acidityLevel = acidityLevel;
        }
    }
}
