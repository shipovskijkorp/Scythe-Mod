package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.util.ScytheCombatUtil;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.item.BloodScytheItem;
import com.shipovskijkorp.scythes.mod.platform.HudSync;
import com.shipovskijkorp.scythes.mod.platform.HudTransport;
import java.util.List;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
//? if >=1.21.11 {
//? } else {
import net.minecraft.item.Item;
//? }
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
//? if >=1.21.11 {
import net.minecraft.server.world.ServerWorld;
//? } else {
//? }
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;

public class BloodHarvestAbility {

    /** Активация способности */
    public static void tryActivate(ServerPlayerEntity player) {

        Hand hand = getHeldBloodScytheHand(player);
        if (hand == null) {
            player.sendMessage(Text.translatable("message.scythes.blood_harvest.no_scythe"), true);
            return;
        }

        ItemStack stack = player.getStackInHand(hand);
//? if >=1.21.11 {
        ServerWorld world = (ServerWorld) player.getEntityWorld();
//? } else {
        Item item = stack.getItem();
//? }

        if (ScytheBalance.BloodHarvest.DURABILITY_COST > 0) {
            int remaining = stack.getMaxDamage() - stack.getDamage();
            if (remaining < ScytheBalance.BloodHarvest.DURABILITY_COST) {
                player.sendMessage(Text.translatable("message.scythes.blood_harvest.no_durability"), true);
                return;
            }
        }

//? if >=1.21.11 {
        if (ScytheCooldowns.remaining(player, ScytheCooldowns.Skill.BLOOD_HARVEST) > 0
                || player.getItemCooldownManager().isCoolingDown(stack)) {
//? } else {
        if (ScytheCooldowns.remaining(player, ScytheCooldowns.Skill.BLOOD_HARVEST) > 0
                || player.getItemCooldownManager().isCoolingDown(item)) {
//? }
            player.sendMessage(Text.translatable("message.scythes.blood_harvest.cooldown"), true);
            return;
        }

        Box box = player.getBoundingBox().expand(ScytheBalance.BloodHarvest.RADIUS);
        List<ServerPlayerEntity> targets =
//? if >=1.21.11 {
                world.getEntitiesByClass(
//? } else {
                player.getWorld().getEntitiesByClass(
//? }
                        ServerPlayerEntity.class,
                        box,
                        p -> p != player && p.isAlive() && !p.isSpectator() && !player.isTeammate(p)
                                && ScytheCombatUtil.isWithinRadius(player, p, ScytheBalance.BloodHarvest.RADIUS)
                );

        if (targets.isEmpty()) {
            player.sendMessage(Text.translatable("message.scythes.blood_harvest.no_targets"), true);
            return;
        }

        // ✅ тратим прочность ТОЛЬКО на активированной косе
        if (ScytheBalance.BloodHarvest.DURABILITY_COST > 0) {
            stack.damage(ScytheBalance.BloodHarvest.DURABILITY_COST, player, hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }

//? if >=1.21.11 {
        player.getItemCooldownManager().set(stack, ScytheBalance.BloodHarvest.COOLDOWN_TICKS);
//? } else {
        player.getItemCooldownManager().set(item, ScytheBalance.BloodHarvest.COOLDOWN_TICKS);
//? }
        ScytheCooldowns.start(player, ScytheCooldowns.Skill.BLOOD_HARVEST, ScytheBalance.BloodHarvest.COOLDOWN_TICKS);

        for (ServerPlayerEntity target : targets) {
            ScytheAdvancementTracker.markBloodActive(player, target);
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, ScytheBalance.BloodHarvest.SLOWNESS_TICKS, ScytheBalance.BloodHarvest.SLOWNESS_AMPLIFIER));
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, ScytheBalance.BloodHarvest.BLINDNESS_TICKS, ScytheBalance.BloodHarvest.BLINDNESS_AMPLIFIER));
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, ScytheBalance.BloodHarvest.WEAKNESS_TICKS, ScytheBalance.BloodHarvest.WEAKNESS_AMPLIFIER));
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, ScytheBalance.BloodHarvest.GLOWING_TICKS, ScytheBalance.BloodHarvest.GLOWING_AMPLIFIER));
        }

        int windowTicks = BloodHarvestTracker.start(
                player,
                targets.stream().map(ServerPlayerEntity::getUuid).toList()
        );
        HudSync.start(player, HudTransport.Timer.BLOOD_HARVEST, windowTicks);

        player.sendMessage(Text.translatable("message.scythes.blood_harvest.success"), true);
    }

    private static Hand getHeldBloodScytheHand(ServerPlayerEntity player) {
        if (player.getMainHandStack().getItem() instanceof BloodScytheItem) return Hand.MAIN_HAND;
        if (player.getOffHandStack().getItem() instanceof BloodScytheItem) return Hand.OFF_HAND;
        return null;
    }
}
