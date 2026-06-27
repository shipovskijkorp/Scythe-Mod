package com.shipovskijkorp.scythes.mod.item;

import com.shipovskijkorp.scythes.mod.ability.GoldenLootMarkTracker;
import com.shipovskijkorp.scythes.mod.ability.GoldenRainAbility;
import com.shipovskijkorp.scythes.mod.ability.GoldenScytheCooldowns;
import com.shipovskijkorp.scythes.mod.ability.ScytheAdvancementTracker;
import com.shipovskijkorp.scythes.mod.client.TooltipUtil;
import com.shipovskijkorp.scythes.mod.util.ScytheDamageTypes;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GoldenScytheItem extends SwordItem {

    public static final int PASSIVE_LOOTING_BONUS = 1;
    public static final int MIDAS_COOLDOWN_TICKS = 20 * 40;
    public static final int MIDAS_DURABILITY_COST = 50;
    public static final float MIDAS_DAMAGE_FRACTION = 0.50F;
    public static final float MIDAS_MIN_DAMAGE = 20.0F;
    public static final float MIDAS_MAX_DAMAGE = 100.0F;
    public static final double MIDAS_REACH = 5.0D;

    public GoldenScytheItem(Settings settings) {
        super(ToolMaterials.NETHERITE, 4, -2.8F, settings);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        TooltipUtil.addWrapped(tooltip, "tooltip.scythes.golden_scythe.desc", Formatting.GRAY, Formatting.ITALIC);

        if (!Screen.hasShiftDown()) {
            TooltipUtil.addHoldShiftHint(tooltip);
            return;
        }

        boolean alt = Screen.hasAltDown();

        if (alt) {
            tooltip.add(Text.empty());
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.golden_scythe.base_stats", Formatting.DARK_GRAY);
        }

        tooltip.add(Text.empty());
        tooltip.add(Text.translatable("tooltip.scythes.section.passive").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.scythes.golden_scythe.passive").formatted(Formatting.GOLD));
        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.golden_scythe.passive.desc", Formatting.DARK_GRAY);
        } else {
            TooltipUtil.addWrapped(
                    tooltip,
                    Text.translatable("tooltip.scythes.stat.looting_bonus", String.valueOf(PASSIVE_LOOTING_BONUS)),
                    Formatting.GOLD
            );
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.stat.piglin_neutrality", Formatting.DARK_GRAY);
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.stat.no_golden_scythe_stacking", Formatting.DARK_GRAY);
        }

        tooltip.add(Text.empty());
        tooltip.add(Text.translatable("tooltip.scythes.section.special").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.scythes.midas_touch").formatted(Formatting.GOLD));
        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.midas_touch.desc", Formatting.GRAY);
        } else {
            TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.cooldown_sec", TooltipUtil.fmtSecondsValue(MIDAS_COOLDOWN_TICKS)), Formatting.GRAY);
            TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.durability_cost", String.valueOf(MIDAS_DURABILITY_COST)), Formatting.GRAY);
            TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.reach_blocks", TooltipUtil.fmtNumber(MIDAS_REACH)), Formatting.GRAY);
            TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.max_health_damage_percent", TooltipUtil.fmtPercentValue(MIDAS_DAMAGE_FRACTION)), Formatting.DARK_GRAY);
            TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.damage_clamp", TooltipUtil.fmtNumber(MIDAS_MIN_DAMAGE), TooltipUtil.fmtNumber(MIDAS_MAX_DAMAGE)), Formatting.DARK_GRAY);
            TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.mark_looting_bonus", String.valueOf(GoldenLootMarkTracker.MARK_LOOTING_BONUS)), Formatting.GOLD);
        }

        tooltip.add(Text.empty());
        tooltip.add(Text.translatable("tooltip.scythes.section.active").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.scythes.golden_rain", TooltipUtil.getScytheAbilityKeyText(Formatting.GOLD)));
        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.golden_rain.desc", Formatting.GRAY);
            TooltipUtil.addHoldAltHint(tooltip);
            return;
        }

        TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.radius_blocks", TooltipUtil.fmtNumber(GoldenRainAbility.RADIUS)), Formatting.GRAY);
        TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.cooldown_sec", TooltipUtil.fmtSecondsValue(GoldenRainAbility.COOLDOWN_TICKS)), Formatting.GRAY);
        TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.durability_cost", String.valueOf(GoldenRainAbility.DURABILITY_COST)), Formatting.GRAY);
        TooltipUtil.addWrapped(tooltip, Text.translatable("tooltip.scythes.stat.mark_looting_bonus", String.valueOf(GoldenLootMarkTracker.MARK_LOOTING_BONUS)), Formatting.GOLD);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (world.isClient) {
            return TypedActionResult.success(stack);
        }

        if (!(user instanceof ServerPlayerEntity player)) {
            return TypedActionResult.pass(stack);
        }

        int cooldownLeft = GoldenScytheCooldowns.getMidasTicksLeft(player);
        if (cooldownLeft > 0) {
            player.sendMessage(Text.translatable("message.scythes.midas_touch.cooldown", Math.max(1, cooldownLeft / 20)), true);
            return TypedActionResult.fail(stack);
        }

        if (!hasEnoughDurability(stack, MIDAS_DURABILITY_COST)) {
            player.sendMessage(Text.translatable("message.scythes.scythe_ability.no_durability"), true);
            return TypedActionResult.fail(stack);
        }

        LivingEntity target = findMidasTarget(player);
        if (target == null) {
            player.sendMessage(Text.translatable("message.scythes.midas_touch.no_target"), true);
            return TypedActionResult.fail(stack);
        }

        GoldenLootMarkTracker.mark(target, player);

        float damage = MathHelper.clamp(
                target.getMaxHealth() * MIDAS_DAMAGE_FRACTION,
                MIDAS_MIN_DAMAGE,
                MIDAS_MAX_DAMAGE
        );
        target.damage(ScytheDamageTypes.midasTouch(world, player), damage);

        stack.damage(MIDAS_DURABILITY_COST, player, p -> p.sendToolBreakStatus(hand));
        GoldenScytheCooldowns.setMidasCooldown(player, MIDAS_COOLDOWN_TICKS);
        ScytheAdvancementTracker.markGoldenSpecial(player);

        world.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                SoundCategory.PLAYERS,
                0.9F,
                0.7F + player.getRandom().nextFloat() * 0.25F
        );

        player.sendMessage(
                target.isAlive()
                        ? Text.translatable("message.scythes.midas_touch.marked")
                        : Text.translatable("message.scythes.midas_touch.killed"),
                true
        );

        return TypedActionResult.success(stack);
    }

    @Nullable
    private static LivingEntity findMidasTarget(ServerPlayerEntity player) {
        Vec3d start = player.getCameraPosVec(1.0F);
        Vec3d direction = player.getRotationVec(1.0F);
        Vec3d end = start.add(direction.multiply(MIDAS_REACH));
        Box searchBox = player.getBoundingBox().stretch(direction.multiply(MIDAS_REACH)).expand(1.0D);

        EntityHitResult hitResult = ProjectileUtil.raycast(
                player,
                start,
                end,
                searchBox,
                entity -> entity instanceof LivingEntity living && isValidGoldenTarget(player, living),
                MIDAS_REACH * MIDAS_REACH
        );

        Entity entity = hitResult == null ? null : hitResult.getEntity();
        return entity instanceof LivingEntity living ? living : null;
    }

    public static boolean isValidGoldenTarget(ServerPlayerEntity player, LivingEntity target) {
        if (target == player) return false;
        if (!target.isAlive()) return false;
        if (target.isSpectator()) return false;
        if (player.isTeammate(target)) return false;
        if (target instanceof TameableEntity tameable && tameable.isTamed()) return false;
        if (target instanceof AbstractHorseEntity horse && horse.isTame()) return false;
        return true;
    }

    public static boolean hasGoldenScythe(PlayerEntity player) {
        return getHeldGoldenScytheHand(player) != null;
    }

    @Nullable
    public static Hand getHeldGoldenScytheHand(PlayerEntity player) {
        if (player.getMainHandStack().getItem() instanceof GoldenScytheItem) {
            return Hand.MAIN_HAND;
        }
        if (player.getOffHandStack().getItem() instanceof GoldenScytheItem) {
            return Hand.OFF_HAND;
        }
        return null;
    }

    public static boolean hasEnoughDurability(ItemStack stack, int cost) {
        if (cost <= 0) return true;
        if (!stack.isDamageable()) return false;
        return stack.getMaxDamage() - stack.getDamage() >= cost;
    }
}
