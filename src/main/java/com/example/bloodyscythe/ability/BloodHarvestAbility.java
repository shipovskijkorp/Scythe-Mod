package com.example.bloodyscythe.ability;

import com.example.bloodyscythe.config.BloodyScytheConfig;
import com.example.bloodyscythe.config.BloodyScytheConfigLoader;
import com.example.bloodyscythe.item.BloodScytheItem;
import com.example.bloodyscythe.network.BloodHarvestHudS2CPacket;
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

    /** Активация способности */
    public static void tryActivate(ServerPlayerEntity player) {

        Hand hand = getHeldBloodScytheHand(player);
        if (hand == null) {
            player.sendMessage(Text.translatable("message.bloodyscythe.blood_harvest.no_scythe"), true);
            return;
        }

        ItemStack stack = player.getStackInHand(hand);
        Item item = stack.getItem();

        // ✅ Цена активации (общая для всех кос): берём из bloodHarvestDurabilityCost
        BloodyScytheConfig config = BloodyScytheConfigLoader.getConfig();
        int cost = Math.max(0, config.bloodHarvestDurabilityCost);

        if (cost > 0) {
            int remaining = stack.getMaxDamage() - stack.getDamage();
            if (remaining < cost) {
                player.sendMessage(Text.translatable("message.bloodyscythe.blood_harvest.no_durability"), true);
                return;
            }
        }

        if (player.getItemCooldownManager().isCoolingDown(item)) {
            player.sendMessage(Text.translatable("message.bloodyscythe.blood_harvest.cooldown"), true);
            return;
        }

        double radius = 10.0;
        int cooldown = 20 * 60;

        radius = config.bloodHarvestRadius;
        cooldown = Math.max(0, config.bloodHarvestCooldownTicks);

        Box box = player.getBoundingBox().expand(radius);
        List<ServerPlayerEntity> targets =
                player.getWorld().getEntitiesByClass(
                        ServerPlayerEntity.class,
                        box,
                        p -> p != player && p.isAlive() && !p.isSpectator() && !player.isTeammate(p)
                );

        if (targets.isEmpty()) {
            player.sendMessage(Text.translatable("message.bloodyscythe.blood_harvest.no_targets"), true);
            return;
        }

        // ✅ тратим прочность ТОЛЬКО на активированной косе
        if (cost > 0) {
            stack.damage(cost, player, p -> p.sendToolBreakStatus(hand));
        }

        // кулдаун по Item
        player.getItemCooldownManager().set(item, cooldown);

        int slowTicks = 20 * 5;
        int blindTicks = 20 * 5;
        int weakTicks = 20 * 5;
        int glowTicks = 20 * 10;
        int slowAmp = 1;
        int weakAmp = 1;

        slowTicks = Math.max(1, config.bloodHarvestSlownessTicks);
        blindTicks = Math.max(1, config.bloodHarvestBlindnessTicks);
        weakTicks = Math.max(1, config.bloodHarvestWeaknessTicks);
        glowTicks = Math.max(1, config.bloodHarvestGlowingTicks);
        slowAmp = Math.max(0, config.bloodHarvestSlownessAmplifier);
        weakAmp = Math.max(0, config.bloodHarvestWeaknessAmplifier);

        for (ServerPlayerEntity target : targets) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, slowTicks, slowAmp));
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, blindTicks, 0));
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, weakTicks, weakAmp));
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, glowTicks, 0));
        }

        int windowTicks = BloodHarvestTracker.start(player);
        BloodHarvestHudS2CPacket.sendTicks(player, windowTicks);

        player.sendMessage(Text.translatable("message.bloodyscythe.blood_harvest.success"), true);
    }

    private static Hand getHeldBloodScytheHand(ServerPlayerEntity player) {
        if (player.getMainHandStack().getItem() instanceof BloodScytheItem) return Hand.MAIN_HAND;
        if (player.getOffHandStack().getItem() instanceof BloodScytheItem) return Hand.OFF_HAND;
        return null;
    }
}
