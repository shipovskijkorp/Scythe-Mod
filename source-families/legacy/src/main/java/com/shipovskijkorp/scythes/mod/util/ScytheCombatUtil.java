package com.shipovskijkorp.scythes.mod.util;

import net.minecraft.server.network.ServerPlayerEntity;
import com.shipovskijkorp.scythes.mod.entity.WitheringMinionEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;

public final class ScytheCombatUtil {

    private ScytheCombatUtil() {
    }

    private static final EquipmentSlot[] ARMOR_SLOTS = new EquipmentSlot[] {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    public static boolean isProtectedWitheringMinion(LivingEntity source, LivingEntity target) {
        if (!(target instanceof WitheringMinionEntity minion) || source == null) return false;
        if (source instanceof ServerPlayerEntity player && minion.isOwner(player)) return true;
        ServerPlayerEntity minionOwner = minion.getOwnerPlayer();
        return minionOwner != null && source.isTeammate(minionOwner);
    }

    public static boolean isInvalidHostileTarget(LivingEntity owner, LivingEntity target) {
        if (isProtectedWitheringMinion(owner, target)) return true;
        if (target == owner) return true;
        if (!target.isAlive()) return true;
        if (target.isSpectator()) return true;
        if (target instanceof VillagerEntity) return true;
        if (target instanceof TameableEntity tameable && tameable.isTamed()) return true;
        if (target instanceof AbstractHorseEntity horse && horse.isTame()) return true;
        return owner != null && owner.isTeammate(target);
    }

    public static void damageArmorSet(LivingEntity target, int amount) {
        if (amount <= 0) return;

        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack armor = target.getEquippedStack(slot);
            if (armor.isEmpty() || !armor.isDamageable()) continue;

            armor.damage(amount, target, entity -> entity.sendEquipmentBreakStatus(slot));
        }
    }

    public static void refreshStatus(LivingEntity target, StatusEffect effect, int durationTicks, int amplifier) {
        target.addStatusEffect(new StatusEffectInstance(
                effect,
                Math.max(1, durationTicks),
                Math.max(0, amplifier),
                false,
                true,
                true
        ));
    }
}
