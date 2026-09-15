package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.item.FireScytheItem;
import com.shipovskijkorp.scythes.mod.util.BurnsUtil;
import com.shipovskijkorp.scythes.mod.util.FireTargeting;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

public final class FireBurstAbility {
    private static final EquipmentSlot[] ARMOR_SLOTS = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };
    private FireBurstAbility() {}

    public static void tryActivate(ServerPlayer player) {
        InteractionHand hand = FireScytheItem.getHeldFireScytheHand(player);
        if (hand == null) {
            player.sendOverlayMessage(Component.translatable("message.scythes.fire_burst.no_scythe"));
            return;
        }
        int cooldown = ScytheCooldowns.remaining(player, ScytheCooldowns.Skill.FIRE_BURST);
        if (cooldown > 0) {
            player.sendOverlayMessage(Component.translatable("message.scythes.fire_burst.cooldown", Math.max(1, (cooldown + 19) / 20)));
            return;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (!FireScytheItem.hasEnoughDurability(stack, ScytheBalance.FireBurst.DURABILITY_COST)) {
            player.sendOverlayMessage(Component.translatable("message.scythes.scythe_ability.no_durability"));
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        double radius = ScytheBalance.FireBurst.RADIUS;
        double radiusSq = radius * radius;
        AABB box = player.getBoundingBox().inflate(radius);
        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class, box,
                target -> FireTargeting.isValidTarget(player, target) && target.distanceToSqr(player) <= radiusSq
        );
        if (targets.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.scythes.fire_burst.no_targets"));
            return;
        }
        for (LivingEntity target : targets) {
            target.igniteForSeconds(ScytheBalance.FireBurst.FIRE_SECONDS);
            BurnsUtil.applyActive(target, ScytheBalance.FireBurst.BURNS_TICKS);
            damageBurnableArmor(target);
            FireLaunchTracker.schedule(target);
        }
        stack.hurtAndBreak(ScytheBalance.FireBurst.DURABILITY_COST, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        ScytheCooldowns.start(player, ScytheCooldowns.Skill.FIRE_BURST, ScytheBalance.FireBurst.COOLDOWN_TICKS);
        ScytheAdvancementTracker.markFireActive(player);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.25F, 0.7F);
        player.sendOverlayMessage(Component.translatable("message.scythes.fire_burst.success", targets.size()));
    }

    private static void damageBurnableArmor(LivingEntity target) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack armor = target.getItemBySlot(slot);
            if (armor.isEmpty() || !armor.isDamageableItem() || isNetherite(armor)) continue;
            armor.hurtAndBreak(ScytheBalance.FireBurst.ARMOR_DAMAGE, target, slot);
        }
    }

    private static boolean isNetherite(ItemStack stack) {
        return stack.is(Items.NETHERITE_HELMET) || stack.is(Items.NETHERITE_CHESTPLATE)
                || stack.is(Items.NETHERITE_LEGGINGS) || stack.is(Items.NETHERITE_BOOTS);
    }
}
