package com.shipovskijkorp.scythes.mod.item;

import com.shipovskijkorp.scythes.mod.util.ScytheCombatUtil;
import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.ability.DamageAttributionTracker;
import com.shipovskijkorp.scythes.mod.ability.ScytheAdvancementTracker;
import com.shipovskijkorp.scythes.mod.ability.ScytheCooldowns;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
//? if >=26.2 {
//? } else {
import net.minecraft.world.entity.EntityType;
//? }
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.TamableAnimal;
//? if >=26.2 {
import net.minecraft.world.entity.npc.villager.Villager;
//? } else {
//? }
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class BloodScytheItem extends ScytheSwordItem {

    public BloodScytheItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);

        if (!(world instanceof ServerLevel serverWorld)) {
            return InteractionResult.SUCCESS;
        }

        if (!(user instanceof ServerPlayer player)) {
            return InteractionResult.PASS;
        }

        List<LivingEntity> targets = findBlenderTargets(serverWorld, player);
        if (targets.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.scythes.blood_blender.no_targets"));
            return InteractionResult.FAIL;
        }

        int cooldownLeft = ScytheCooldowns.remaining(player, ScytheCooldowns.Skill.BLENDER);
        if (cooldownLeft > 0) {
            player.sendOverlayMessage(Component.translatable("message.scythes.blood_blender.cooldown", Math.max(1, cooldownLeft / 20)));
            return InteractionResult.FAIL;
        }

        if (!hasEnoughDurability(stack, ScytheBalance.Blood.BLENDER_DURABILITY_COST)) {
            player.sendOverlayMessage(Component.translatable("message.scythes.scythe_ability.no_durability"));
            return InteractionResult.FAIL;
        }

        for (LivingEntity target : targets) {
            ScytheAdvancementTracker.markBloodSpecial(player, target);
            pullTowardPlayer(player, target);
            DamageSource source = player.damageSources().playerAttack(player);
            float damage = calculateBlenderDamage(serverWorld, stack, target, source);
            target.hurtServer(serverWorld, source, damage);
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ScytheBalance.Blood.BLENDER_SLOWNESS_TICKS, ScytheBalance.Blood.BLENDER_SLOWNESS_AMPLIFIER, false, true, true));
            target.addEffect(new MobEffectInstance(ScytheMod.BLEEDING, ScytheBalance.Blood.BLENDER_BLEEDING_TICKS, ScytheBalance.Blood.BLENDER_BLEEDING_AMPLIFIER, false, true, true));
            DamageAttributionTracker.recordBleeding(target, player, ScytheBalance.Blood.BLENDER_BLEEDING_TICKS);
        }

        stack.hurtAndBreak(ScytheBalance.Blood.BLENDER_DURABILITY_COST, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        ScytheCooldowns.start(player, ScytheCooldowns.Skill.BLENDER, ScytheBalance.Blood.BLENDER_COOLDOWN_TICKS);
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.95F, 0.55F + player.getRandom().nextFloat() * 0.2F);
        player.sendOverlayMessage(Component.translatable("message.scythes.blood_blender.success"));

        return InteractionResult.SUCCESS;
    }

    private static List<LivingEntity> findBlenderTargets(ServerLevel world, ServerPlayer player) {
        AABB box = player.getBoundingBox().inflate(ScytheBalance.Blood.BLENDER_RADIUS);
        double maxDistanceSquared = ScytheBalance.Blood.BLENDER_RADIUS * ScytheBalance.Blood.BLENDER_RADIUS;
        return world.getEntitiesOfClass(LivingEntity.class, box, target -> !isInvalidBlenderTarget(player, target) && target.distanceToSqr(player) <= maxDistanceSquared);
    }

    private static boolean isInvalidBlenderTarget(ServerPlayer player, LivingEntity target) {
        if (ScytheCombatUtil.isProtectedWitheringMinion(player, target)) return true;
        if (target == player) return true;
        if (!target.isAlive()) return true;
        if (target.isSpectator()) return true;
        if (player.isAlliedTo(target)) return true;
        if (target instanceof Player) return false;

        if (target instanceof TamableAnimal tameable && tameable.isTame()) return true;
        if (target instanceof AgeableMob) return true;
//? if >=26.2 {
        if (target instanceof Villager) return true;
//? } else {
        if (target.getType() == EntityType.VILLAGER) return true;
//? }
        if (target instanceof NeutralMob) return true;

        return target.getType().getCategory() != MobCategory.MONSTER;
    }

    private static void pullTowardPlayer(ServerPlayer player, LivingEntity target) {
        Vec3 toPlayer = new Vec3(player.getX() - target.getX(), player.getY(0.5D) - target.getY(0.5D), player.getZ() - target.getZ());
        double distance = toPlayer.length();
        if (distance < 0.001D) return;

        Vec3 pull = toPlayer.normalize().scale(ScytheBalance.Blood.BLENDER_PULL_BASE + distance * ScytheBalance.Blood.BLENDER_PULL_PER_BLOCK);
        target.push(pull.x, Math.max(ScytheBalance.Blood.BLENDER_PULL_Y, pull.y), pull.z);
    }

    private static float calculateBlenderDamage(ServerLevel world, ItemStack stack, LivingEntity target, DamageSource source) {
        float singleHit = EnchantmentHelper.modifyDamage(world, stack, target, source, ScytheBalance.Blood.BLENDER_SINGLE_HIT_DAMAGE);
        return Math.max(0.0F, singleHit) * ScytheBalance.Blood.BLENDER_HIT_COUNT;
    }

    public static boolean hasEnoughDurability(ItemStack stack, int cost) {
        if (cost <= 0) return true;
        if (!stack.isDamageableItem()) return false;
        return stack.getMaxDamage() - stack.getDamageValue() >= cost;
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.hurtEnemy(stack, target, attacker);
        if (ScytheCombatUtil.isProtectedWitheringMinion(attacker, target)) return;

        if (attacker.level().isClientSide()) {
            return;
        }

        if (attacker.getRandom().nextDouble() > ScytheBalance.Blood.BLEEDING_CHANCE) {
            return;
        }

        int spikedLevel = getSpikedBladeLevel(attacker, stack);
        int duration = ScytheBalance.Blood.BLEEDING_BASE_DURATION_TICKS * (1 + Math.max(0, spikedLevel));

        MobEffectInstance current = target.getEffect(ScytheMod.BLEEDING);
        if (current != null) {
            duration = Math.max(duration, current.getDuration() + ScytheBalance.Blood.BLEEDING_EXTEND_TICKS);
        }

        target.addEffect(new MobEffectInstance(ScytheMod.BLEEDING, duration, ScytheBalance.Blood.BLEEDING_AMPLIFIER, false, true));
        if (attacker instanceof ServerPlayer player) {
            ScytheAdvancementTracker.markBloodPassive(player, target);
            DamageAttributionTracker.recordBleeding(target, player, duration);
        }
    }

    private static int getSpikedBladeLevel(LivingEntity holder, ItemStack stack) {
        Optional<Holder.Reference<Enchantment>> entry = holder.level().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(ScytheMod.SPIKED_BLADE);
        return entry.map(enchantment -> EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack)).orElse(0);
    }
}
