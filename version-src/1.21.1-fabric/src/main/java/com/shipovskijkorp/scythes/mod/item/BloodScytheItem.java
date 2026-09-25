package com.shipovskijkorp.scythes.mod.item;

import com.shipovskijkorp.scythes.mod.util.ScytheCombatUtil;
import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.ability.BloodScytheAttackContext;
import com.shipovskijkorp.scythes.mod.ability.DamageAttributionTracker;
import com.shipovskijkorp.scythes.mod.ability.ScytheAdvancementTracker;
import com.shipovskijkorp.scythes.mod.ability.ScytheCooldowns;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import java.util.List;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class BloodScytheItem extends ScytheSwordItem {

    public BloodScytheItem(Settings settings) {
        super(settings);
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

        List<LivingEntity> targets = findBlenderTargets(player);
        if (targets.isEmpty()) {
            player.sendMessage(Text.translatable("message.scythes.blood_blender.no_targets"), true);
            return TypedActionResult.fail(stack);
        }

        int cooldownLeft = ScytheCooldowns.remaining(player, ScytheCooldowns.Skill.BLENDER);
        if (cooldownLeft > 0) {
            player.sendMessage(Text.translatable("message.scythes.blood_blender.cooldown", Math.max(1, cooldownLeft / 20)), true);
            return TypedActionResult.fail(stack);
        }

        if (!hasEnoughDurability(stack, ScytheBalance.Blood.BLENDER_DURABILITY_COST)) {
            player.sendMessage(Text.translatable("message.scythes.scythe_ability.no_durability"), true);
            return TypedActionResult.fail(stack);
        }

        for (LivingEntity target : targets) {
            ScytheAdvancementTracker.markBloodSpecial(player, target);
            pullTowardPlayer(player, target);
            float damage = calculateBlenderDamage(player, stack, target);
            BloodScytheAttackContext.enter(player.getUuid());
            try {
                target.damage(player.getDamageSources().playerAttack(player), damage);
            } finally {
                BloodScytheAttackContext.exit();
            }
            target.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.SLOWNESS,
                    ScytheBalance.Blood.BLENDER_SLOWNESS_TICKS,
                    ScytheBalance.Blood.BLENDER_SLOWNESS_AMPLIFIER,
                    false,
                    true,
                    true
            ));
            target.addStatusEffect(new StatusEffectInstance(
                    ScytheMod.BLEEDING,
                    ScytheBalance.Blood.BLENDER_BLEEDING_TICKS,
                    ScytheBalance.Blood.BLENDER_BLEEDING_AMPLIFIER,
                    false,
                    true,
                    true
            ));
            DamageAttributionTracker.recordBleeding(target, player, ScytheBalance.Blood.BLENDER_BLEEDING_TICKS);
        }

        stack.damage(ScytheBalance.Blood.BLENDER_DURABILITY_COST, player, hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        ScytheCooldowns.start(player, ScytheCooldowns.Skill.BLENDER, ScytheBalance.Blood.BLENDER_COOLDOWN_TICKS);
        world.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
                SoundCategory.PLAYERS,
                0.95F,
                0.55F + player.getRandom().nextFloat() * 0.2F
        );
        player.sendMessage(Text.translatable("message.scythes.blood_blender.success"), true);

        return TypedActionResult.success(stack);
    }

    private static List<LivingEntity> findBlenderTargets(ServerPlayerEntity player) {
        Box box = player.getBoundingBox().expand(ScytheBalance.Blood.BLENDER_RADIUS);
        double maxDistanceSquared = ScytheBalance.Blood.BLENDER_RADIUS * ScytheBalance.Blood.BLENDER_RADIUS;
        return player.getWorld().getEntitiesByClass(
                LivingEntity.class,
                box,
                target -> !isInvalidBlenderTarget(player, target)
                        && target.squaredDistanceTo(player) <= maxDistanceSquared
        );
    }

    private static boolean isInvalidBlenderTarget(ServerPlayerEntity player, LivingEntity target) {
        if (ScytheCombatUtil.isProtectedWitheringMinion(player, target)) return true;
        if (target == player) return true;
        if (!target.isAlive()) return true;
        if (target.isSpectator()) return true;
        if (player.isTeammate(target)) return true;
        if (target instanceof PlayerEntity) return false;

        if (target instanceof TameableEntity tameable && tameable.isTamed()) return true;
        if (target instanceof AbstractHorseEntity horse && horse.isTame()) return true;
        if (target instanceof PassiveEntity) return true;
        if (target instanceof VillagerEntity) return true;
        if (target instanceof Angerable) return true;

        return target.getType().getSpawnGroup() != SpawnGroup.MONSTER;
    }

    private static void pullTowardPlayer(ServerPlayerEntity player, LivingEntity target) {
        Vec3d toPlayer = new Vec3d(
                player.getX() - target.getX(),
                player.getBodyY(0.5D) - target.getBodyY(0.5D),
                player.getZ() - target.getZ()
        );

        double distance = toPlayer.length();
        if (distance < 0.001D) return;

        Vec3d pull = toPlayer.normalize().multiply(ScytheBalance.Blood.BLENDER_PULL_BASE + distance * ScytheBalance.Blood.BLENDER_PULL_PER_BLOCK);
        target.addVelocity(pull.x, Math.max(ScytheBalance.Blood.BLENDER_PULL_Y, pull.y), pull.z);
        target.velocityModified = true;
    }

    private static float calculateBlenderDamage(ServerPlayerEntity player, ItemStack stack, LivingEntity target) {
        float damage = ScytheBalance.Blood.BLENDER_SINGLE_HIT_DAMAGE;
        damage = EnchantmentHelper.getDamage((ServerWorld) player.getWorld(), stack, target, player.getDamageSources().playerAttack(player), damage);
        return Math.max(0.0F, damage) * ScytheBalance.Blood.BLENDER_HIT_COUNT;
    }

    public static boolean hasEnoughDurability(ItemStack stack, int cost) {
        if (cost <= 0) return true;
        if (!stack.isDamageable()) return false;
        return stack.getMaxDamage() - stack.getDamage() >= cost;
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (ScytheCombatUtil.isProtectedWitheringMinion(attacker, target)) return super.postHit(stack, target, attacker);
        if (attacker.getWorld().isClient) {
            return super.postHit(stack, target, attacker);
        }

        if (attacker.getRandom().nextDouble() > ScytheBalance.Blood.BLEEDING_CHANCE) {
            return super.postHit(stack, target, attacker);
        }

        int spikedLevel = attacker.getRegistryManager()
                .getWrapperOrThrow(RegistryKeys.ENCHANTMENT)
                .getOptional(ScytheMod.SPIKED_BLADE)
                .map(enchantment -> EnchantmentHelper.getLevel(enchantment, stack))
                .orElse(0);
        int duration = ScytheBalance.Blood.BLEEDING_BASE_DURATION_TICKS * (1 + Math.max(0, spikedLevel));

        StatusEffectInstance current = target.getStatusEffect(ScytheMod.BLEEDING);
        if (current != null) {
            duration = Math.max(duration, current.getDuration() + ScytheBalance.Blood.BLEEDING_EXTEND_TICKS);
        }

        target.addStatusEffect(new StatusEffectInstance(
                ScytheMod.BLEEDING,
                duration,
                ScytheBalance.Blood.BLEEDING_AMPLIFIER,
                false,
                true
        ));

        if (attacker instanceof ServerPlayerEntity player) {
            ScytheAdvancementTracker.markBloodPassive(player, target);
            DamageAttributionTracker.recordBleeding(target, player, duration);
        }

        return super.postHit(stack, target, attacker);
    }
}
