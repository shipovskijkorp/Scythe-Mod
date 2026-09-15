package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.item.FireScytheItem;
import com.shipovskijkorp.scythes.mod.util.BurnsUtil;
import com.shipovskijkorp.scythes.mod.util.FireTargeting;
import java.util.List;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
//? if >=1.21.11 {
import net.minecraft.server.world.ServerWorld;
//? } else {
//? }
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;

public final class FireBurstAbility {
    private static final EquipmentSlot[] ARMOR_SLOTS = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };
    private FireBurstAbility() {}

    public static void tryActivate(ServerPlayerEntity player) {
        Hand hand = FireScytheItem.getHeldFireScytheHand(player);
        if (hand == null) {
            player.sendMessage(Text.translatable("message.scythes.fire_burst.no_scythe"), true);
            return;
        }
        int cooldown = ScytheCooldowns.remaining(player, ScytheCooldowns.Skill.FIRE_BURST);
        if (cooldown > 0) {
            player.sendMessage(Text.translatable("message.scythes.fire_burst.cooldown", Math.max(1, (cooldown + 19) / 20)), true);
            return;
        }
        ItemStack stack = player.getStackInHand(hand);
        if (!FireScytheItem.hasEnoughDurability(stack, ScytheBalance.FireBurst.DURABILITY_COST)) {
            player.sendMessage(Text.translatable("message.scythes.scythe_ability.no_durability"), true);
            return;
        }
        double radius = ScytheBalance.FireBurst.RADIUS;
        double radiusSq = radius * radius;
        Box box = player.getBoundingBox().expand(radius);
//? if >=1.21.11 {
        ServerWorld world = player.getEntityWorld();
        List<LivingEntity> targets = world.getEntitiesByClass(
//? } else {
        List<LivingEntity> targets = player.getWorld().getEntitiesByClass(
//? }
                LivingEntity.class, box,
                target -> FireTargeting.isValidTarget(player, target) && target.squaredDistanceTo(player) <= radiusSq
        );
        if (targets.isEmpty()) {
            player.sendMessage(Text.translatable("message.scythes.fire_burst.no_targets"), true);
            return;
        }
        for (LivingEntity target : targets) {
            target.setOnFireFor(ScytheBalance.FireBurst.FIRE_SECONDS);
            BurnsUtil.applyActive(target, ScytheBalance.FireBurst.BURNS_TICKS);
            damageBurnableArmor(target);
            FireLaunchTracker.schedule(target);
        }
        stack.damage(ScytheBalance.FireBurst.DURABILITY_COST, player, hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        ScytheCooldowns.start(player, ScytheCooldowns.Skill.FIRE_BURST, ScytheBalance.FireBurst.COOLDOWN_TICKS);
        ScytheAdvancementTracker.markFireActive(player);
//? if >=1.21.11 {
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.25F, 0.7F);
//? } else {
        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.25F, 0.7F);
//? }
        player.sendMessage(Text.translatable("message.scythes.fire_burst.success", targets.size()), true);
    }

    private static void damageBurnableArmor(LivingEntity target) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack armor = target.getEquippedStack(slot);
            if (armor.isEmpty() || !armor.isDamageable() || isNetherite(armor)) continue;
            armor.damage(ScytheBalance.FireBurst.ARMOR_DAMAGE, target, slot);
        }
    }

    private static boolean isNetherite(ItemStack stack) {
        return stack.isOf(Items.NETHERITE_HELMET) || stack.isOf(Items.NETHERITE_CHESTPLATE)
                || stack.isOf(Items.NETHERITE_LEGGINGS) || stack.isOf(Items.NETHERITE_BOOTS);
    }
}
