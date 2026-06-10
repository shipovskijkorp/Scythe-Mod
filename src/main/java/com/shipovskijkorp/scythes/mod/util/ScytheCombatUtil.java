package com.shipovskijkorp.scythes.mod.util;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
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

    public static boolean isInvalidHostileTarget(LivingEntity owner, LivingEntity target) {
        if (target == owner) return true;
        if (!target.isAlive()) return true;
        if (target.isSpectator()) return true;
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
