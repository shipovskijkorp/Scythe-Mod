package com.shipovskijkorp.scythes.mod.item;

import com.shipovskijkorp.scythes.mod.ability.GoldenLootMarkTracker;
import com.shipovskijkorp.scythes.mod.ability.GoldenRainAbility;
import com.shipovskijkorp.scythes.mod.ability.GoldenScytheCooldowns;
import com.shipovskijkorp.scythes.mod.ability.ScytheAdvancementTracker;
import com.shipovskijkorp.scythes.mod.client.TooltipUtil;
import com.shipovskijkorp.scythes.mod.util.ScytheDamageTypes;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class GoldenScytheItem extends ScytheSwordItem {

    public static final int PASSIVE_LOOTING_BONUS = 1;
    public static final int MIDAS_COOLDOWN_TICKS = 20 * 40;
    public static final int MIDAS_DURABILITY_COST = 50;
    public static final float MIDAS_DAMAGE_FRACTION = 0.50F;
    public static final float MIDAS_MIN_DAMAGE = 20.0F;
    public static final float MIDAS_MAX_DAMAGE = 100.0F;
    public static final double MIDAS_REACH = 5.0D;

    public GoldenScytheItem(Properties settings) {
        super(settings);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        List<Component> tooltip = new ArrayList<>();
        TooltipUtil.addWrapped(tooltip, "tooltip.scythes.golden_scythe.desc", ChatFormatting.GRAY, ChatFormatting.ITALIC);

        if (!TooltipUtil.isShiftDown()) {
            TooltipUtil.addHoldShiftHint(tooltip);
            TooltipUtil.flush(tooltip, textConsumer);
            return;
        }

        boolean alt = TooltipUtil.isAltDown();

        if (alt) {
            tooltip.add(Component.empty());
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.golden_scythe.base_stats", ChatFormatting.DARK_GRAY);
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.scythes.section.passive").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.scythes.golden_scythe.passive").withStyle(ChatFormatting.GOLD));
        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.golden_scythe.passive.desc", ChatFormatting.DARK_GRAY);
        } else {
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.looting_bonus", String.valueOf(PASSIVE_LOOTING_BONUS)), ChatFormatting.GOLD);
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.stat.piglin_neutrality", ChatFormatting.DARK_GRAY);
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.stat.no_golden_scythe_stacking", ChatFormatting.DARK_GRAY);
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.scythes.section.special").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.scythes.midas_touch").withStyle(ChatFormatting.GOLD));
        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.midas_touch.desc", ChatFormatting.GRAY);
        } else {
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.cooldown_sec", TooltipUtil.fmtSecondsValue(MIDAS_COOLDOWN_TICKS)), ChatFormatting.GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.durability_cost", String.valueOf(MIDAS_DURABILITY_COST)), ChatFormatting.GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.reach_blocks", TooltipUtil.fmtNumber(MIDAS_REACH)), ChatFormatting.GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.max_health_damage_percent", TooltipUtil.fmtPercentValue(MIDAS_DAMAGE_FRACTION)), ChatFormatting.DARK_GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.damage_clamp", TooltipUtil.fmtNumber(MIDAS_MIN_DAMAGE), TooltipUtil.fmtNumber(MIDAS_MAX_DAMAGE)), ChatFormatting.DARK_GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.mark_looting_bonus", String.valueOf(GoldenLootMarkTracker.MARK_LOOTING_BONUS)), ChatFormatting.GOLD);
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.scythes.section.active").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.scythes.golden_rain", TooltipUtil.getScytheAbilityKeyText(ChatFormatting.GOLD)));
        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.golden_rain.desc", ChatFormatting.GRAY);
            TooltipUtil.addHoldAltHint(tooltip);
            TooltipUtil.flush(tooltip, textConsumer);
            return;
        }

        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.radius_blocks", TooltipUtil.fmtNumber(GoldenRainAbility.RADIUS)), ChatFormatting.GRAY);
        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.cooldown_sec", TooltipUtil.fmtSecondsValue(GoldenRainAbility.COOLDOWN_TICKS)), ChatFormatting.GRAY);
        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.durability_cost", String.valueOf(GoldenRainAbility.DURABILITY_COST)), ChatFormatting.GRAY);
        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.mark_looting_bonus", String.valueOf(GoldenLootMarkTracker.MARK_LOOTING_BONUS)), ChatFormatting.GOLD);

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

        int cooldownLeft = GoldenScytheCooldowns.getMidasTicksLeft(player);
        if (cooldownLeft > 0) {
            player.sendOverlayMessage(Component.translatable("message.scythes.midas_touch.cooldown", Math.max(1, cooldownLeft / 20)));
            return InteractionResult.FAIL;
        }

        if (!hasEnoughDurability(stack, MIDAS_DURABILITY_COST)) {
            player.sendOverlayMessage(Component.translatable("message.scythes.scythe_ability.no_durability"));
            return InteractionResult.FAIL;
        }

        LivingEntity target = findMidasTarget(player);
        if (target == null) {
            player.sendOverlayMessage(Component.translatable("message.scythes.midas_touch.no_target"));
            return InteractionResult.FAIL;
        }

        GoldenLootMarkTracker.mark(target, player);

        float damage = Mth.clamp(
                target.getMaxHealth() * MIDAS_DAMAGE_FRACTION,
                MIDAS_MIN_DAMAGE,
                MIDAS_MAX_DAMAGE
        );
        target.hurtServer(serverWorld, ScytheDamageTypes.midasTouch(serverWorld, player), damage);

        stack.hurtAndBreak(MIDAS_DURABILITY_COST, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        GoldenScytheCooldowns.setMidasCooldown(player, MIDAS_COOLDOWN_TICKS);
        ScytheAdvancementTracker.markGoldenSpecial(player);

        world.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS,
                0.9F,
                0.7F + player.getRandom().nextFloat() * 0.25F
        );

        player.sendOverlayMessage(
                target.isAlive()
                        ? Component.translatable("message.scythes.midas_touch.marked")
                        : Component.translatable("message.scythes.midas_touch.killed")
        );

        return InteractionResult.SUCCESS;
    }

    private static LivingEntity findMidasTarget(ServerPlayer player) {
        Vec3 start = player.getEyePosition(1.0F);
        Vec3 direction = player.getLookAngle().normalize();
        Vec3 end = start.add(direction.scale(MIDAS_REACH));
        AABB searchBox = player.getBoundingBox().expandTowards(direction.scale(MIDAS_REACH)).inflate(1.0D);

        return player.level().getEntitiesOfClass(
                        LivingEntity.class,
                        searchBox,
                        target -> isValidGoldenTarget(player, target)
                )
                .stream()
                .map(target -> new MidasCandidate(target, getHitDistance(start, end, target)))
                .filter(candidate -> candidate.hitDistance().isPresent())
                .min(Comparator.comparingDouble(candidate -> candidate.hitDistance().orElse(Double.MAX_VALUE)))
                .map(MidasCandidate::target)
                .orElse(null);
    }

    private static Optional<Double> getHitDistance(Vec3 start, Vec3 end, LivingEntity target) {
        AABB box = target.getBoundingBox().inflate(0.35D);
        return box.clip(start, end).map(hit -> hit.distanceToSqr(start));
    }

    public static boolean isValidGoldenTarget(ServerPlayer player, LivingEntity target) {
        if (target == player) return false;
        if (!target.isAlive()) return false;
        if (target.isSpectator()) return false;
        if (player.isAlliedTo(target)) return false;
        if (target instanceof TamableAnimal tameable && tameable.isTame()) return false;
        if (target instanceof AbstractHorse horse && horse.isTamed()) return false;
        // Golden Scythe is loot-focused: other peaceful mobs and villagers remain valid targets.
        return true;
    }

    public static boolean hasGoldenScythe(Player player) {
        return player.getMainHandItem().getItem() instanceof GoldenScytheItem
                || player.getOffhandItem().getItem() instanceof GoldenScytheItem;
    }

    public static InteractionHand getHeldGoldenScytheHand(Player player) {
        if (player.getMainHandItem().getItem() instanceof GoldenScytheItem) return InteractionHand.MAIN_HAND;
        if (player.getOffhandItem().getItem() instanceof GoldenScytheItem) return InteractionHand.OFF_HAND;
        return null;
    }

    public static boolean hasEnoughDurability(ItemStack stack, int cost) {
        if (cost <= 0) return true;
        if (!stack.isDamageableItem()) return false;
        return stack.getMaxDamage() - stack.getDamageValue() >= cost;
    }

    private record MidasCandidate(LivingEntity target, Optional<Double> hitDistance) {
    }
}
