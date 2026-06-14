package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.item.BloodScytheItem;
import com.shipovskijkorp.scythes.mod.network.BloodHarvestHudS2CPacket;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;

import java.util.List;

public class BloodHarvestAbility {

    public static final double RADIUS = 10.0D;
    public static final int COOLDOWN_TICKS = 20 * 60;
    public static final int DURABILITY_COST = 100;

    public static final int SLOWNESS_TICKS = 20 * 10;
    public static final int BLINDNESS_TICKS = 20 * 10;
    public static final int WEAKNESS_TICKS = 20 * 10;
    public static final int GLOWING_TICKS = 20 * 20;
    public static final int SLOWNESS_AMPLIFIER = 1;
    public static final int WEAKNESS_AMPLIFIER = 1;

    /** Активация способности */
    public static void tryActivate(ServerPlayerEntity player) {

        Hand hand = getHeldBloodScytheHand(player);
        if (hand == null) {
            player.sendMessage(Text.translatable("message.scythes.blood_harvest.no_scythe"), true);
            return;
        }

        ItemStack stack = player.getStackInHand(hand);
        Item item = stack.getItem();

        if (DURABILITY_COST > 0) {
            int remaining = stack.getMaxDamage() - stack.getDamage();
            if (remaining < DURABILITY_COST) {
                player.sendMessage(Text.translatable("message.scythes.blood_harvest.no_durability"), true);
                return;
            }
        }

        if (player.getItemCooldownManager().isCoolingDown(item)) {
            player.sendMessage(Text.translatable("message.scythes.blood_harvest.cooldown"), true);
            return;
        }

        Box box = player.getBoundingBox().expand(RADIUS);
        List<ServerPlayerEntity> targets =
                player.getWorld().getEntitiesByClass(
                        ServerPlayerEntity.class,
                        box,
                        p -> p != player && p.isAlive() && !p.isSpectator() && !player.isTeammate(p)
                );

        if (targets.isEmpty()) {
            player.sendMessage(Text.translatable("message.scythes.blood_harvest.no_targets"), true);
            return;
        }

        // ✅ тратим прочность ТОЛЬКО на активированной косе
        if (DURABILITY_COST > 0) {
            stack.damage(DURABILITY_COST, player, p -> p.sendToolBreakStatus(hand));
        }

        // кулдаун по Item
        player.getItemCooldownManager().set(item, COOLDOWN_TICKS);

        for (ServerPlayerEntity target : targets) {
            ScytheAdvancementTracker.markBloodActive(player, target);
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, SLOWNESS_TICKS, SLOWNESS_AMPLIFIER));
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, BLINDNESS_TICKS, 0));
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, WEAKNESS_TICKS, WEAKNESS_AMPLIFIER));
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, GLOWING_TICKS, 0));
        }

        int windowTicks = BloodHarvestTracker.start(player);
        BloodHarvestHudS2CPacket.sendTicks(player, windowTicks);

        player.sendMessage(Text.translatable("message.scythes.blood_harvest.success"), true);
    }

    private static Hand getHeldBloodScytheHand(ServerPlayerEntity player) {
        if (player.getMainHandStack().getItem() instanceof BloodScytheItem) return Hand.MAIN_HAND;
        if (player.getOffHandStack().getItem() instanceof BloodScytheItem) return Hand.OFF_HAND;
        return null;
    }
}
