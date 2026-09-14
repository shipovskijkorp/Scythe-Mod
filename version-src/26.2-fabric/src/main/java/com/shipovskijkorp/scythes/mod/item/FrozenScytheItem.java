package com.shipovskijkorp.scythes.mod.item;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.ability.FrozenScytheCooldowns;
import com.shipovskijkorp.scythes.mod.ability.FrozenStormAbility;
import com.shipovskijkorp.scythes.mod.ability.ScytheAdvancementTracker;
import com.shipovskijkorp.scythes.mod.client.TooltipUtil;
import com.shipovskijkorp.scythes.mod.entity.IceSpikeEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class FrozenScytheItem extends ScytheSwordItem {

    public static final int ICE_SPIKE_DURABILITY_COST = 30;
    public static final int ICE_SPIKE_COOLDOWN_TICKS = 20 * 20;
    public static final float ICE_SPIKE_SPEED = 3.0F;

    public static final double COLD_MASTER_FREEZING_CHANCE = 0.33D;
    public static final int COLD_MASTER_FREEZING_TICKS = 20;
    public static final int COLD_MASTER_FROST_WALKER_LEVEL = 2;

    public FrozenScytheItem(Properties properties) {
        super(properties);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> textConsumer, TooltipFlag type) {
        List<Component> tooltip = new ArrayList<>();
        TooltipUtil.addWrapped(tooltip, "tooltip.scythes.frozen_scythe.desc", ChatFormatting.GRAY, ChatFormatting.ITALIC);

        if (!TooltipUtil.isShiftDown()) {
            TooltipUtil.addHoldShiftHint(tooltip);
            TooltipUtil.flush(tooltip, textConsumer);
            return;
        }

        boolean alt = TooltipUtil.isAltDown();
        if (alt) {
            tooltip.add(Component.empty());
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.frozen_scythe.base_stats", ChatFormatting.DARK_GRAY);
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.scythes.section.passive").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.scythes.frozen_scythe.passive").withStyle(ChatFormatting.AQUA));
        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.frozen_scythe.passive.desc", ChatFormatting.DARK_GRAY);
        } else {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.stat.powder_snow_walk", ChatFormatting.DARK_GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.frost_walker_level", COLD_MASTER_FROST_WALKER_LEVEL), ChatFormatting.DARK_GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.freezing_chance_percent", TooltipUtil.fmtPercentValue(COLD_MASTER_FREEZING_CHANCE)), ChatFormatting.DARK_GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.freezing_sec", TooltipUtil.fmtSecondsValue(COLD_MASTER_FREEZING_TICKS)), ChatFormatting.DARK_GRAY);
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.scythes.section.special").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.scythes.ice_spike").withStyle(ChatFormatting.AQUA));
        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.ice_spike.desc", ChatFormatting.GRAY);
        } else {
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.cooldown_sec", TooltipUtil.fmtSecondsValue(ICE_SPIKE_COOLDOWN_TICKS)), ChatFormatting.GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.durability_cost", String.valueOf(ICE_SPIKE_DURABILITY_COST)), ChatFormatting.GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.damage", TooltipUtil.fmtNumber(IceSpikeEntity.HIT_DAMAGE)), ChatFormatting.DARK_GRAY);
            TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.freezing_sec", TooltipUtil.fmtSecondsValue(IceSpikeEntity.FREEZING_TICKS)), ChatFormatting.DARK_GRAY);
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.scythes.section.active").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.scythes.frozen_storm", TooltipUtil.getScytheAbilityKeyText(ChatFormatting.AQUA)));
        if (!alt) {
            TooltipUtil.addWrapped(tooltip, "tooltip.scythes.frozen_storm.desc", ChatFormatting.GRAY);
            TooltipUtil.addHoldAltHint(tooltip);
            TooltipUtil.flush(tooltip, textConsumer);
            return;
        }

        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.radius_blocks", TooltipUtil.fmtNumber(FrozenStormAbility.RADIUS)), ChatFormatting.GRAY);
        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.cooldown_sec", TooltipUtil.fmtSecondsValue(FrozenStormAbility.COOLDOWN_TICKS)), ChatFormatting.GRAY);
        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.durability_cost", String.valueOf(FrozenStormAbility.DURABILITY_COST)), ChatFormatting.GRAY);
        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.freezing_sec", TooltipUtil.fmtSecondsValue(FrozenStormAbility.FREEZING_TICKS)), ChatFormatting.DARK_GRAY);
        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.slowness_sec", TooltipUtil.fmtSecondsValue(FrozenStormAbility.SLOWNESS_TICKS)), ChatFormatting.DARK_GRAY);
        TooltipUtil.addWrapped(tooltip, Component.translatable("tooltip.scythes.stat.weakness_sec", TooltipUtil.fmtSecondsValue(FrozenStormAbility.WEAKNESS_TICKS)), ChatFormatting.DARK_GRAY);
        TooltipUtil.addWrapped(tooltip, "tooltip.scythes.stat.ignores_pets_and_teammates", ChatFormatting.DARK_GRAY);
        TooltipUtil.flush(tooltip, textConsumer);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        if (!(entity instanceof Player player)) return;
        if (!isTheOnlyHeldStackToTick(player, stack)) return;
        freezeNearbyWater(player, level, COLD_MASTER_FROST_WALKER_LEVEL);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.hurtEnemy(stack, target, attacker);
        if (attacker.level().isClientSide()) return;
        if (target.isAlive() && attacker.getRandom().nextDouble() < COLD_MASTER_FREEZING_CHANCE) {
            target.addEffect(new MobEffectInstance(ScytheMod.FREEZING, COLD_MASTER_FREEZING_TICKS, 0));
            if (attacker instanceof ServerPlayer player) {
                ScytheAdvancementTracker.markFrozenPassive(player);
            }
        }
    }

    public static boolean isInPlayerInventory(Player player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).getItem() instanceof FrozenScytheItem) return true;
        }
        return false;
    }

    @Nullable
    public static InteractionHand getHeldFrozenScytheHand(Player player) {
        if (player.getMainHandItem().getItem() instanceof FrozenScytheItem) return InteractionHand.MAIN_HAND;
        if (player.getOffhandItem().getItem() instanceof FrozenScytheItem) return InteractionHand.OFF_HAND;
        return null;
    }

    private static boolean isTheOnlyHeldStackToTick(Player player, ItemStack stack) {
        if (player.getMainHandItem() == stack) return true;
        return !(player.getMainHandItem().getItem() instanceof FrozenScytheItem) && player.getOffhandItem() == stack;
    }

    private static void freezeNearbyWater(Player player, ServerLevel level, int frostLevel) {
        if (!player.onGround() || player.isPassenger() || frostLevel <= 0) return;

        int radius = Math.min(16, 2 + frostLevel);
        double radiusSquared = radius * radius;
        BlockPos center = player.blockPosition();
        BlockState frostedIce = Blocks.FROSTED_ICE.defaultBlockState();

        for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
            for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                BlockPos waterPos = new BlockPos(x, center.getY() - 1, z);
                double dx = waterPos.getX() + 0.5D - player.getX();
                double dy = waterPos.getY() + 0.5D - player.getY();
                double dz = waterPos.getZ() + 0.5D - player.getZ();
                if (dx * dx + dy * dy + dz * dz >= radiusSquared) continue;

                BlockState state = level.getBlockState(waterPos);
                FluidState fluid = state.getFluidState();
                if (!level.getBlockState(waterPos.above()).isAir()) continue;
                if (!state.is(Blocks.WATER)) continue;
                if (!fluid.is(FluidTags.WATER) || !fluid.isSource()) continue;
                if (!level.noCollision(new AABB(waterPos))) continue;

                if (level.setBlock(waterPos, frostedIce, 3)) {
                    // FrostedIceBlock.onPlace schedules the initial melt tick.
                    level.gameEvent(player, GameEvent.BLOCK_PLACE, waterPos);
                }
            }
        }
    }

    @Override
    public InteractionResult use(Level level, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;
        if (!(user instanceof ServerPlayer player)) return InteractionResult.PASS;

        int cooldownLeft = FrozenScytheCooldowns.getIceSpikeTicksLeft(player);
        if (cooldownLeft > 0) {
            player.sendOverlayMessage(Component.translatable("message.scythes.ice_spike.cooldown", Math.max(1, (cooldownLeft + 19) / 20)));
            return InteractionResult.FAIL;
        }
        if (!hasEnoughDurability(stack, ICE_SPIKE_DURABILITY_COST)) {
            player.sendOverlayMessage(Component.translatable("message.scythes.scythe_ability.no_durability"));
            return InteractionResult.FAIL;
        }

        IceSpikeEntity spike = new IceSpikeEntity(serverLevel, player, stack);
        spike.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, ICE_SPIKE_SPEED, 0.0F);
        serverLevel.addFreshEntity(spike);

        stack.hurtAndBreak(ICE_SPIKE_DURABILITY_COST, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        FrozenScytheCooldowns.setIceSpikeCooldown(player, ICE_SPIKE_COOLDOWN_TICKS);
        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F, 0.75F + player.getRandom().nextFloat() * 0.15F);
        return InteractionResult.SUCCESS;
    }

    public static boolean hasEnoughDurability(ItemStack stack, int cost) {
        if (cost <= 0) return true;
        if (!stack.isDamageableItem()) return false;
        return stack.getMaxDamage() - stack.getDamageValue() >= cost;
    }
}
