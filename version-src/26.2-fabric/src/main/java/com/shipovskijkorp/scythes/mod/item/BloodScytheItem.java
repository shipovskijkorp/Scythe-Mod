package com.shipovskijkorp.scythes.mod.item;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.ability.BloodHarvestAbility;
import com.shipovskijkorp.scythes.mod.ability.BloodHarvestTracker;
import com.shipovskijkorp.scythes.mod.ability.BloodScytheCooldowns;
import com.shipovskijkorp.scythes.mod.ability.BloodScytheVampirism;
import com.shipovskijkorp.scythes.mod.ability.DamageAttributionTracker;
import com.shipovskijkorp.scythes.mod.ability.ScytheAdvancementTracker;
import com.shipovskijkorp.scythes.mod.client.TooltipUtil;
import com.shipovskijkorp.scythes.mod.effect.BleedingEffect;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class BloodScytheItem extends ScytheSwordItem {

    public static final double BLEEDING_CHANCE = 0.40D;
    public static final double DEFENSE_PIERCE_CHANCE = 0.20D;
    public static final double DEFENSE_PIERCE_MITIGATION_IGNORED = 0.50D;
    public static final int BLEEDING_BASE_DURATION_TICKS = 20 * 2;
    public static final int BLEEDING_EXTEND_TICKS = 20 * 2;

    public static final double BLENDER_RADIUS = 3.0D;
    public static final int BLENDER_COOLDOWN_TICKS = 20 * 30;
    public static final int BLENDER_DURABILITY_COST = 50;
    public static final int BLENDER_HIT_COUNT = 2;
    public static final int BLENDER_SLOWNESS_TICKS = 20 * 4;
    public static final int BLENDER_SLOWNESS_AMPLIFIER = 1;
    public static final int BLENDER_BLEEDING_TICKS = 20 * 10;
    public static final int BLENDER_BLEEDING_AMPLIFIER = 1;
    private static final float BLENDER_SINGLE_HIT_DAMAGE = 1.0F + 4.0F + 4.0F;
    private static final double BLENDER_PULL_BASE = 0.45D;
    private static final double BLENDER_PULL_PER_BLOCK = 0.18D;
    private static final double BLENDER_PULL_Y = 0.18D;

    public BloodScytheItem(Properties settings) {
        super(settings);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        List<Component> tooltip = new ArrayList<>();
        TooltipUtil.addWrapped(tooltip, "tooltip.scythes.bloody_scythe.desc", ChatFormatting.GRAY, ChatFormatting.ITALIC);

        if (!TooltipUtil.isShiftDown()) {
            TooltipUtil.addHoldShiftHint(tooltip);
            TooltipUtil.flush(tooltip, textConsumer);
            return;
        }

        boolean alt = TooltipUtil.isAltDown();

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.scythes.section.passive").withStyle(ChatFormatting.GRAY));

        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.bleeding_chance", ChatFormatting.DARK_RED);
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.bloody_scythe.passive.desc", ChatFormatting.DARK_GRAY);
        } else {
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.bleed_chance_percent", TooltipUtil.fmtPercentValue(BLEEDING_CHANCE)), ChatFormatting.DARK_RED);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.bleed_base_sec", TooltipUtil.fmtSecondsValue(BLEEDING_BASE_DURATION_TICKS)), ChatFormatting.DARK_GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.bleed_extend_sec", TooltipUtil.fmtSecondsValue(BLEEDING_EXTEND_TICKS)), ChatFormatting.DARK_GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.bleeding_damage_per_second", TooltipUtil.fmtNumber(BleedingEffect.DAMAGE_PER_SECOND)), ChatFormatting.DARK_GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.vampirism_chance_percent", TooltipUtil.fmtPercentValue(BloodScytheVampirism.VAMPIRISM_CHANCE)), ChatFormatting.DARK_RED);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.vampirism_heal_percent", TooltipUtil.fmtPercentValue(BloodScytheVampirism.HEAL_FRACTION)), ChatFormatting.DARK_GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.vampirism_cooldown_sec", TooltipUtil.fmtSecondsValue(BloodScytheVampirism.COOLDOWN_TICKS)), ChatFormatting.DARK_GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.defense_pierce", TooltipUtil.fmtPercentValue(DEFENSE_PIERCE_CHANCE), TooltipUtil.fmtPercentValue(DEFENSE_PIERCE_MITIGATION_IGNORED)), ChatFormatting.DARK_RED);
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.scythes.section.special").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.scythes.blood_blender").withStyle(ChatFormatting.DARK_RED));
        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.blood_blender.desc", ChatFormatting.GRAY);
        } else {
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.radius_blocks", TooltipUtil.fmtNumber(BLENDER_RADIUS)), ChatFormatting.GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.cooldown_sec", TooltipUtil.fmtSecondsValue(BLENDER_COOLDOWN_TICKS)), ChatFormatting.GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.durability_cost", String.valueOf(BLENDER_DURABILITY_COST)), ChatFormatting.GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.basic_hit_count", String.valueOf(BLENDER_HIT_COUNT)), ChatFormatting.DARK_GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.slowness_ii_sec", TooltipUtil.fmtSecondsValue(BLENDER_SLOWNESS_TICKS)), ChatFormatting.DARK_GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.bleeding_ii_sec", TooltipUtil.fmtSecondsValue(BLENDER_BLEEDING_TICKS)), ChatFormatting.DARK_GRAY);
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.scythes.section.active").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.scythes.blood_harvest", TooltipUtil.getScytheAbilityKeyText(ChatFormatting.DARK_RED)));

        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.blood_harvest.desc", ChatFormatting.GRAY);
            TooltipUtil.addHoldAltHint(tooltip);
            TooltipUtil.flush(tooltip, textConsumer);
            return;
        }

        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.radius_blocks", TooltipUtil.fmtNumber(BloodHarvestAbility.RADIUS)), ChatFormatting.GRAY);
        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.cooldown_sec", TooltipUtil.fmtSecondsValue(BloodHarvestAbility.COOLDOWN_TICKS)), ChatFormatting.GRAY);
        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.durability_cost", String.valueOf(BloodHarvestAbility.DURABILITY_COST)), ChatFormatting.GRAY);
        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.kill_window_sec", TooltipUtil.fmtSecondsValue(BloodHarvestTracker.KILL_WINDOW_TICKS)), ChatFormatting.GRAY);
        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.slowness_sec", TooltipUtil.fmtSecondsValue(BloodHarvestAbility.SLOWNESS_TICKS)), ChatFormatting.DARK_GRAY);
        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.blindness_sec", TooltipUtil.fmtSecondsValue(BloodHarvestAbility.BLINDNESS_TICKS)), ChatFormatting.DARK_GRAY);
        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.weakness_sec", TooltipUtil.fmtSecondsValue(BloodHarvestAbility.WEAKNESS_TICKS)), ChatFormatting.DARK_GRAY);
        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.glowing_sec", TooltipUtil.fmtSecondsValue(BloodHarvestAbility.GLOWING_TICKS)), ChatFormatting.DARK_GRAY);
        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.success_buffs_sec", TooltipUtil.fmtSecondsValue(BloodHarvestTracker.SUCCESS_BUFF_TICKS)), ChatFormatting.DARK_GRAY);
        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.failure_debuffs_sec", TooltipUtil.fmtSecondsValue(BloodHarvestTracker.FAILURE_DEBUFF_TICKS)), ChatFormatting.DARK_GRAY);

        TooltipUtil.flush(tooltip, textConsumer);
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

        int cooldownLeft = BloodScytheCooldowns.getBlenderTicksLeft(player);
        if (cooldownLeft > 0) {
            player.sendOverlayMessage(Component.translatable("message.scythes.blood_blender.cooldown", Math.max(1, cooldownLeft / 20)));
            return InteractionResult.FAIL;
        }

        if (!hasEnoughDurability(stack, BLENDER_DURABILITY_COST)) {
            player.sendOverlayMessage(Component.translatable("message.scythes.scythe_ability.no_durability"));
            return InteractionResult.FAIL;
        }

        for (LivingEntity target : targets) {
            ScytheAdvancementTracker.markBloodSpecial(player, target);
            pullTowardPlayer(player, target);
            DamageSource source = player.damageSources().playerAttack(player);
            float damage = calculateBlenderDamage(serverWorld, stack, target, source);
            target.hurtServer(serverWorld, source, damage);
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, BLENDER_SLOWNESS_TICKS, BLENDER_SLOWNESS_AMPLIFIER, false, true, true));
            target.addEffect(new MobEffectInstance(ScytheMod.BLEEDING, BLENDER_BLEEDING_TICKS, BLENDER_BLEEDING_AMPLIFIER, false, true, true));
            DamageAttributionTracker.recordBleeding(target, player, BLENDER_BLEEDING_TICKS);
        }

        stack.hurtAndBreak(BLENDER_DURABILITY_COST, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        BloodScytheCooldowns.setBlenderCooldown(player, BLENDER_COOLDOWN_TICKS);
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.95F, 0.55F + player.getRandom().nextFloat() * 0.2F);
        player.sendOverlayMessage(Component.translatable("message.scythes.blood_blender.success"));

        return InteractionResult.SUCCESS;
    }

    private static List<LivingEntity> findBlenderTargets(ServerLevel world, ServerPlayer player) {
        AABB box = player.getBoundingBox().inflate(BLENDER_RADIUS);
        double maxDistanceSquared = BLENDER_RADIUS * BLENDER_RADIUS;
        return world.getEntitiesOfClass(LivingEntity.class, box, target -> !isInvalidBlenderTarget(player, target) && target.distanceToSqr(player) <= maxDistanceSquared);
    }

    private static boolean isInvalidBlenderTarget(ServerPlayer player, LivingEntity target) {
        if (target == player) return true;
        if (!target.isAlive()) return true;
        if (target.isSpectator()) return true;
        if (player.isAlliedTo(target)) return true;
        if (target instanceof Player) return false;

        if (target instanceof TamableAnimal tameable && tameable.isTame()) return true;
        if (target instanceof AgeableMob) return true;
        if (target instanceof Villager) return true;
        if (target instanceof NeutralMob) return true;

        return target.getType().getCategory() != MobCategory.MONSTER;
    }

    private static void pullTowardPlayer(ServerPlayer player, LivingEntity target) {
        Vec3 toPlayer = new Vec3(player.getX() - target.getX(), player.getY(0.5D) - target.getY(0.5D), player.getZ() - target.getZ());
        double distance = toPlayer.length();
        if (distance < 0.001D) return;

        Vec3 pull = toPlayer.normalize().scale(BLENDER_PULL_BASE + distance * BLENDER_PULL_PER_BLOCK);
        target.push(pull.x, Math.max(BLENDER_PULL_Y, pull.y), pull.z);
    }

    private static float calculateBlenderDamage(ServerLevel world, ItemStack stack, LivingEntity target, DamageSource source) {
        float singleHit = EnchantmentHelper.modifyDamage(world, stack, target, source, BLENDER_SINGLE_HIT_DAMAGE);
        return Math.max(0.0F, singleHit) * BLENDER_HIT_COUNT;
    }

    public static boolean hasEnoughDurability(ItemStack stack, int cost) {
        if (cost <= 0) return true;
        if (!stack.isDamageableItem()) return false;
        return stack.getMaxDamage() - stack.getDamageValue() >= cost;
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.hurtEnemy(stack, target, attacker);

        if (attacker.level().isClientSide()) {
            return;
        }

        if (attacker.getRandom().nextDouble() > BLEEDING_CHANCE) {
            return;
        }

        int spikedLevel = getSpikedBladeLevel(attacker, stack);
        int duration = BLEEDING_BASE_DURATION_TICKS * (1 + Math.max(0, spikedLevel));

        MobEffectInstance current = target.getEffect(ScytheMod.BLEEDING);
        if (current != null) {
            duration = Math.max(duration, current.getDuration() + BLEEDING_EXTEND_TICKS);
        }

        target.addEffect(new MobEffectInstance(ScytheMod.BLEEDING, duration, 0, false, true));
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
