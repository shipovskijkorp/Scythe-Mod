package com.shipovskijkorp.scythes.mod.item;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.ability.ScytheAdvancementTracker;
import com.shipovskijkorp.scythes.mod.ability.ScytheCooldowns;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.entity.ToxicOrbEntity;
import com.shipovskijkorp.scythes.mod.util.ScytheCombatUtil;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class ToxicScytheItem extends ScytheSwordItem {

    public ToxicScytheItem(Settings settings) {
        super(settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient) {
            ServerPlayerEntity player = attacker instanceof ServerPlayerEntity serverPlayer ? serverPlayer : null;
            boolean passiveTriggered = false;

            if (attacker.getRandom().nextDouble() < ScytheBalance.Toxic.PASSIVE_ARMOR_DAMAGE_CHANCE) {
                int armorDamage = applyAcidityArmorDamageBonus(ScytheBalance.Toxic.PASSIVE_ARMOR_DAMAGE, getAcidityLevel(attacker, stack), attacker.getRandom());
                ScytheCombatUtil.damageArmorSet(target, armorDamage);
                passiveTriggered = true;
            }

            if (attacker.getRandom().nextDouble() < ScytheBalance.Toxic.PASSIVE_POISON_CHANCE) {
                ScytheCombatUtil.refreshStatus(target, StatusEffects.POISON, ScytheBalance.Toxic.PASSIVE_POISON_TICKS, ScytheBalance.Toxic.PASSIVE_POISON_AMPLIFIER);
                if (player != null) {
                    ScytheAdvancementTracker.recordToxicPoison(player, target, ScytheBalance.Toxic.PASSIVE_POISON_TICKS);
                }
                passiveTriggered = true;
            }

            if (player != null && passiveTriggered) {
                ScytheAdvancementTracker.markToxicPassive(player);
            }
        }

        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, net.minecraft.entity.player.PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (world.isClient) {
            return TypedActionResult.success(stack);
        }

        if (!(user instanceof ServerPlayerEntity player)) {
            return TypedActionResult.pass(stack);
        }

        int cooldownLeft = ScytheCooldowns.remaining(player, ScytheCooldowns.Skill.TOXIC_ORB);
        if (cooldownLeft > 0) {
            player.sendMessage(Text.translatable("message.scythes.toxic_orb.cooldown", Math.max(1, cooldownLeft / 20)), true);
            return TypedActionResult.fail(stack);
        }

        if (!hasEnoughDurability(stack, ScytheBalance.Toxic.ORB_DURABILITY_COST)) {
            player.sendMessage(Text.translatable("message.scythes.scythe_ability.no_durability"), true);
            return TypedActionResult.fail(stack);
        }

        ToxicOrbEntity orb = new ToxicOrbEntity(world, player, getAcidityLevel(player, stack));
        orb.refreshPositionAndAngles(player.getX(), player.getEyeY() - ScytheBalance.ToxicOrb.SPAWN_EYE_OFFSET, player.getZ(), player.getYaw(), player.getPitch());
        orb.setVelocity(player, player.getPitch(), player.getYaw(), 0.0F, ScytheBalance.Toxic.ORB_SPEED, 0.0F);
        world.spawnEntity(orb);

        stack.damage(ScytheBalance.Toxic.ORB_DURABILITY_COST, player, hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        ScytheCooldowns.start(player, ScytheCooldowns.Skill.TOXIC_ORB, ScytheBalance.Toxic.ORB_COOLDOWN_TICKS);
        ScytheAdvancementTracker.markToxicSpecial(player);

        world.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ENTITY_BLAZE_SHOOT,
                SoundCategory.PLAYERS,
                0.75F,
                0.85F + player.getRandom().nextFloat() * 0.3F
        );

        return TypedActionResult.success(stack);
    }

    public static int getAcidityLevel(LivingEntity holder, ItemStack stack) {
        return Math.max(0, Math.min(3, holder.getRegistryManager()
                .getWrapperOrThrow(RegistryKeys.ENCHANTMENT)
                .getOptional(ScytheMod.ACIDITY)
                .map(enchantment -> EnchantmentHelper.getLevel(enchantment, stack))
                .orElse(0)));
    }

    public static int applyAcidityArmorDamageBonus(int baseAmount, int acidityLevel, Random random) {
        if (baseAmount <= 0) return 0;
        int level = Math.max(0, Math.min(ScytheBalance.Enchantments.ACIDITY_MAX_LEVEL, acidityLevel));
        if (level <= 0) return baseAmount;

        double exactAmount = baseAmount * (1.0D + ScytheBalance.Toxic.ACIDITY_ARMOR_DAMAGE_BONUS_PER_LEVEL * level);
        int amount = (int) Math.floor(exactAmount);
        double fractional = exactAmount - amount;
        if (random != null && fractional > 0.0D && random.nextDouble() < fractional) {
            amount++;
        }
        return Math.max(baseAmount, amount);
    }

    public static boolean hasEnoughDurability(ItemStack stack, int cost) {
        if (cost <= 0) return true;
        if (!stack.isDamageable()) return false;
        return stack.getMaxDamage() - stack.getDamage() >= cost;
    }
}
