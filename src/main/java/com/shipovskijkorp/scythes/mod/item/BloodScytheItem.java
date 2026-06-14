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
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BloodScytheItem extends SwordItem {

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
    private static final float BLENDER_SINGLE_HIT_DAMAGE = 1.0F + ToolMaterials.NETHERITE.getAttackDamage() + 4.0F;
    private static final double BLENDER_PULL_BASE = 0.45D;
    private static final double BLENDER_PULL_PER_BLOCK = 0.18D;
    private static final double BLENDER_PULL_Y = 0.18D;

    public BloodScytheItem(Settings settings) {
        super(ToolMaterials.NETHERITE, 4, -2.8F, settings);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        TooltipUtil.addWrapped(tooltip, "tooltip.scythes.bloody_scythe.desc", Formatting.GRAY, Formatting.ITALIC);

        if (!Screen.hasShiftDown()) {
            TooltipUtil.addHoldShiftHint(tooltip);
            return;
        }

        boolean alt = Screen.hasAltDown();

        tooltip.add(Text.empty());
        tooltip.add(Text.translatable("tooltip.scythes.section.passive").formatted(Formatting.GRAY));

        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.bleeding_chance", Formatting.DARK_RED);
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.bloody_scythe.passive.desc", Formatting.DARK_GRAY);
        } else {
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable(
                            "tooltip.scythes.stat.bleed_chance_percent",
                            TooltipUtil.fmtPercentValue(BLEEDING_CHANCE)
                    ),
                    Formatting.DARK_RED
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable(
                            "tooltip.scythes.stat.bleed_base_sec",
                            TooltipUtil.fmtSecondsValue(BLEEDING_BASE_DURATION_TICKS)
                    ),
                    Formatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable(
                            "tooltip.scythes.stat.bleed_extend_sec",
                            TooltipUtil.fmtSecondsValue(BLEEDING_EXTEND_TICKS)
                    ),
                    Formatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable(
                            "tooltip.scythes.stat.bleeding_damage_per_second",
                            TooltipUtil.fmtNumber(BleedingEffect.DAMAGE_PER_SECOND)
                    ),
                    Formatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable(
                            "tooltip.scythes.stat.vampirism_chance_percent",
                            TooltipUtil.fmtPercentValue(BloodScytheVampirism.VAMPIRISM_CHANCE)
                    ),
                    Formatting.DARK_RED
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable(
                            "tooltip.scythes.stat.vampirism_heal_percent",
                            TooltipUtil.fmtPercentValue(BloodScytheVampirism.HEAL_FRACTION)
                    ),
                    Formatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable(
                            "tooltip.scythes.stat.vampirism_cooldown_sec",
                            TooltipUtil.fmtSecondsValue(BloodScytheVampirism.COOLDOWN_TICKS)
                    ),
                    Formatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable(
                            "tooltip.scythes.stat.defense_pierce",
                            TooltipUtil.fmtPercentValue(DEFENSE_PIERCE_CHANCE),
                            TooltipUtil.fmtPercentValue(DEFENSE_PIERCE_MITIGATION_IGNORED)
                    ),
                    Formatting.DARK_RED
            );
        }

        tooltip.add(Text.empty());
        tooltip.add(Text.translatable("tooltip.scythes.section.special").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.scythes.blood_blender").formatted(Formatting.DARK_RED));
        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.blood_blender.desc", Formatting.GRAY);
        } else {
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.radius_blocks", TooltipUtil.fmtNumber(BLENDER_RADIUS)),
                    Formatting.GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.cooldown_sec", TooltipUtil.fmtSecondsValue(BLENDER_COOLDOWN_TICKS)),
                    Formatting.GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.durability_cost", String.valueOf(BLENDER_DURABILITY_COST)),
                    Formatting.GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.basic_hit_count", String.valueOf(BLENDER_HIT_COUNT)),
                    Formatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.slowness_ii_sec", TooltipUtil.fmtSecondsValue(BLENDER_SLOWNESS_TICKS)),
                    Formatting.DARK_GRAY
            );
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.bleeding_ii_sec", TooltipUtil.fmtSecondsValue(BLENDER_BLEEDING_TICKS)),
                    Formatting.DARK_GRAY
            );
        }

        tooltip.add(Text.empty());
        tooltip.add(Text.translatable("tooltip.scythes.section.active").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.scythes.blood_harvest", TooltipUtil.getScytheAbilityKeyText(Formatting.DARK_RED)));

        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.blood_harvest.desc", Formatting.GRAY);
            TooltipUtil.addHoldAltHint(tooltip);
            return;
        }

        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.radius_blocks", TooltipUtil.fmtNumber(BloodHarvestAbility.RADIUS)),
                Formatting.GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.cooldown_sec", TooltipUtil.fmtSecondsValue(BloodHarvestAbility.COOLDOWN_TICKS)),
                Formatting.GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.durability_cost", String.valueOf(BloodHarvestAbility.DURABILITY_COST)),
                Formatting.GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.kill_window_sec", TooltipUtil.fmtSecondsValue(BloodHarvestTracker.KILL_WINDOW_TICKS)),
                Formatting.GRAY
        );

        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.slowness_sec", TooltipUtil.fmtSecondsValue(BloodHarvestAbility.SLOWNESS_TICKS)),
                Formatting.DARK_GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.blindness_sec", TooltipUtil.fmtSecondsValue(BloodHarvestAbility.BLINDNESS_TICKS)),
                Formatting.DARK_GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.weakness_sec", TooltipUtil.fmtSecondsValue(BloodHarvestAbility.WEAKNESS_TICKS)),
                Formatting.DARK_GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.glowing_sec", TooltipUtil.fmtSecondsValue(BloodHarvestAbility.GLOWING_TICKS)),
                Formatting.DARK_GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.success_buffs_sec", TooltipUtil.fmtSecondsValue(BloodHarvestTracker.SUCCESS_BUFF_TICKS)),
                Formatting.DARK_GRAY
        );
        TooltipUtil.addWrapped(
                tooltip,
                Text.translatable("tooltip.scythes.stat.failure_debuffs_sec", TooltipUtil.fmtSecondsValue(BloodHarvestTracker.FAILURE_DEBUFF_TICKS)),
                Formatting.DARK_GRAY
        );
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

        int cooldownLeft = BloodScytheCooldowns.getBlenderTicksLeft(player);
        if (cooldownLeft > 0) {
            player.sendMessage(Text.translatable("message.scythes.blood_blender.cooldown", Math.max(1, cooldownLeft / 20)), true);
            return TypedActionResult.fail(stack);
        }

        if (!hasEnoughDurability(stack, BLENDER_DURABILITY_COST)) {
            player.sendMessage(Text.translatable("message.scythes.scythe_ability.no_durability"), true);
            return TypedActionResult.fail(stack);
        }

        for (LivingEntity target : targets) {
            ScytheAdvancementTracker.markBloodSpecial(player, target);
            pullTowardPlayer(player, target);
            float damage = calculateBlenderDamage(stack, target);
            target.damage(player.getDamageSources().playerAttack(player), damage);
            target.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.SLOWNESS,
                    BLENDER_SLOWNESS_TICKS,
                    BLENDER_SLOWNESS_AMPLIFIER,
                    false,
                    true,
                    true
            ));
            target.addStatusEffect(new StatusEffectInstance(
                    ScytheMod.BLEEDING,
                    BLENDER_BLEEDING_TICKS,
                    BLENDER_BLEEDING_AMPLIFIER,
                    false,
                    true,
                    true
            ));
            DamageAttributionTracker.recordBleeding(target, player, BLENDER_BLEEDING_TICKS);
        }

        stack.damage(BLENDER_DURABILITY_COST, player, p -> p.sendToolBreakStatus(hand));
        BloodScytheCooldowns.setBlenderCooldown(player, BLENDER_COOLDOWN_TICKS);
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
        Box box = player.getBoundingBox().expand(BLENDER_RADIUS);
        double maxDistanceSquared = BLENDER_RADIUS * BLENDER_RADIUS;
        return player.getWorld().getEntitiesByClass(
                LivingEntity.class,
                box,
                target -> !isInvalidBlenderTarget(player, target)
                        && target.squaredDistanceTo(player) <= maxDistanceSquared
        );
    }

    private static boolean isInvalidBlenderTarget(ServerPlayerEntity player, LivingEntity target) {
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

        Vec3d pull = toPlayer.normalize().multiply(BLENDER_PULL_BASE + distance * BLENDER_PULL_PER_BLOCK);
        target.addVelocity(pull.x, Math.max(BLENDER_PULL_Y, pull.y), pull.z);
        target.velocityModified = true;
    }

    private static float calculateBlenderDamage(ItemStack stack, LivingEntity target) {
        float damage = BLENDER_SINGLE_HIT_DAMAGE;
        damage += EnchantmentHelper.getAttackDamage(stack, target.getGroup());
        return Math.max(0.0F, damage) * BLENDER_HIT_COUNT;
    }

    public static boolean hasEnoughDurability(ItemStack stack, int cost) {
        if (cost <= 0) return true;
        if (!stack.isDamageable()) return false;
        return stack.getMaxDamage() - stack.getDamage() >= cost;
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.getWorld().isClient) {
            return super.postHit(stack, target, attacker);
        }

        if (attacker.getRandom().nextDouble() > BLEEDING_CHANCE) {
            return super.postHit(stack, target, attacker);
        }

        int spikedLevel = EnchantmentHelper.getLevel(ScytheMod.SPIKED_BLADE, stack);
        int duration = BLEEDING_BASE_DURATION_TICKS * (1 + Math.max(0, spikedLevel));

        StatusEffectInstance current = target.getStatusEffect(ScytheMod.BLEEDING);
        if (current != null) {
            duration = Math.max(duration, current.getDuration() + BLEEDING_EXTEND_TICKS);
        }

        target.addStatusEffect(new StatusEffectInstance(
                ScytheMod.BLEEDING,
                duration,
                0,
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
