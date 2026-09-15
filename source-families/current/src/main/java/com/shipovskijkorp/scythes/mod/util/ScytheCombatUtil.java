package com.shipovskijkorp.scythes.mod.util;

import net.minecraft.server.level.ServerPlayer;
import com.shipovskijkorp.scythes.mod.entity.WitheringMinionEntity;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
//? if >=26.2 {
//? } else {
import net.minecraft.world.entity.EntityType;
//? }
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
//? if >=26.2 {
import net.minecraft.world.entity.npc.villager.Villager;
//? } else {
//? }
import net.minecraft.world.item.ItemStack;

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
        if (source instanceof ServerPlayer player && minion.isOwner(player)) return true;
        ServerPlayer minionOwner = minion.getOwnerPlayer();
        return minionOwner != null && source.isAlliedTo(minionOwner);
    }

    public static boolean isInvalidHostileTarget(LivingEntity owner, LivingEntity target) {
        if (isProtectedWitheringMinion(owner, target)) return true;
        if (target == owner) return true;
        if (!target.isAlive()) return true;
        if (target.isSpectator()) return true;
//? if >=26.2 {
        if (target instanceof Villager) return true;
//? } else {
        if (target.getType() == EntityType.VILLAGER) return true;
//? }
        if (target instanceof TamableAnimal tameable && tameable.isTame()) return true;
        if (target instanceof AbstractHorse horse && horse.isTamed()) return true;
        return owner != null && owner.isAlliedTo(target);
    }

    public static void damageArmorSet(LivingEntity target, int amount) {
        if (amount <= 0) return;

        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack armor = target.getItemBySlot(slot);
            if (armor.isEmpty() || !armor.isDamageableItem()) continue;

            armor.hurtAndBreak(amount, target, slot);
        }
    }

    public static void refreshStatus(LivingEntity target, Holder<MobEffect> effect, int durationTicks, int amplifier) {
        target.addEffect(new MobEffectInstance(
                effect,
                Math.max(1, durationTicks),
                Math.max(0, amplifier),
                false,
                true,
                true
        ));
    }
}
