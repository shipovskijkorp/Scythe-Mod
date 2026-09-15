package com.shipovskijkorp.scythes.mod.item;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.ability.ScytheAdvancementTracker;
import com.shipovskijkorp.scythes.mod.ability.ScytheCooldowns;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.entity.ToxicOrbEntity;
import com.shipovskijkorp.scythes.mod.util.ScytheCombatUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ToxicScytheItem extends ScytheSwordItem {

    public ToxicScytheItem(Properties settings) {
        super(settings);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.hurtEnemy(stack, target, attacker);

        boolean passiveTriggered = false;
        int acidityLevel = getAcidityLevel(attacker, stack);

        if (attacker.getRandom().nextDouble() < ScytheBalance.Toxic.PASSIVE_ARMOR_DAMAGE_CHANCE) {
            ScytheCombatUtil.damageArmorSet(target, applyAcidityBonus(attacker.getRandom(), ScytheBalance.Toxic.PASSIVE_ARMOR_DAMAGE, acidityLevel));
            passiveTriggered = true;
        }

        if (attacker.getRandom().nextDouble() < ScytheBalance.Toxic.PASSIVE_POISON_CHANCE) {
            ScytheCombatUtil.refreshStatus(target, MobEffects.POISON, ScytheBalance.Toxic.PASSIVE_POISON_TICKS, ScytheBalance.Toxic.PASSIVE_POISON_AMPLIFIER);
            if (attacker instanceof ServerPlayer player) {
                ScytheAdvancementTracker.recordToxicPoison(player, target, ScytheBalance.Toxic.PASSIVE_POISON_TICKS);
            }
            passiveTriggered = true;
        }

        if (passiveTriggered && attacker instanceof ServerPlayer player) {
            ScytheAdvancementTracker.markToxicPassive(player);
        }
    }

    @Override
    public InteractionResult use(Level world, net.minecraft.world.entity.player.Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);

        if (!(world instanceof ServerLevel)) {
            return InteractionResult.SUCCESS;
        }

        if (!(user instanceof ServerPlayer player)) {
            return InteractionResult.PASS;
        }

        int cooldownLeft = ScytheCooldowns.remaining(player, ScytheCooldowns.Skill.TOXIC_ORB);
        if (cooldownLeft > 0) {
            player.sendOverlayMessage(Component.translatable("message.scythes.toxic_orb.cooldown", Math.max(1, cooldownLeft / 20)));
            return InteractionResult.FAIL;
        }

        if (!hasEnoughDurability(stack, ScytheBalance.Toxic.ORB_DURABILITY_COST)) {
            player.sendOverlayMessage(Component.translatable("message.scythes.scythe_ability.no_durability"));
            return InteractionResult.FAIL;
        }

        ToxicOrbEntity orb = new ToxicOrbEntity(world, player);
        orb.setAcidityLevel(getAcidityLevel(player, stack));
        orb.snapTo(player.getX(), player.getEyeY() - ScytheBalance.ToxicOrb.SPAWN_EYE_OFFSET, player.getZ(), player.getYRot(), player.getXRot());
        orb.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, ScytheBalance.Toxic.ORB_SPEED, 0.0F);
        world.addFreshEntity(orb);

        stack.hurtAndBreak(ScytheBalance.Toxic.ORB_DURABILITY_COST, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        ScytheCooldowns.start(player, ScytheCooldowns.Skill.TOXIC_ORB, ScytheBalance.Toxic.ORB_COOLDOWN_TICKS);
        ScytheAdvancementTracker.markToxicSpecial(player);

        world.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.BLAZE_SHOOT,
                SoundSource.PLAYERS,
                0.75F,
                0.85F + player.getRandom().nextFloat() * 0.3F
        );

        return InteractionResult.SUCCESS;
    }

    public static int getAcidityLevel(LivingEntity holder, ItemStack stack) {
        java.util.Optional<net.minecraft.core.Holder.Reference<net.minecraft.world.item.enchantment.Enchantment>> entry = holder.level().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(ScytheMod.ACIDITY);
        return entry.map(enchantment -> net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack)).orElse(0);
    }

    public static int applyAcidityBonus(RandomSource random, int baseArmorDamage, int acidityLevel) {
        if (baseArmorDamage <= 0 || acidityLevel <= 0) return baseArmorDamage;

        double exactBonus = baseArmorDamage * ScytheBalance.Toxic.ACIDITY_ARMOR_DAMAGE_BONUS_PER_LEVEL * Math.min(ScytheBalance.Enchantments.ACIDITY_MAX_LEVEL, acidityLevel);
        int wholeBonus = (int) Math.floor(exactBonus);
        double fractionalBonus = exactBonus - wholeBonus;
        if (fractionalBonus > 0.0D && random.nextDouble() < fractionalBonus) {
            wholeBonus++;
        }

        return baseArmorDamage + wholeBonus;
    }

    public static boolean hasEnoughDurability(ItemStack stack, int cost) {
        if (cost <= 0) return true;
        if (!stack.isDamageableItem()) return false;
        return stack.getMaxDamage() - stack.getDamageValue() >= cost;
    }
}
