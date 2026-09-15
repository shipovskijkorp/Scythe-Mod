package com.shipovskijkorp.scythes.mod.item;

import com.shipovskijkorp.scythes.mod.util.ScytheCombatUtil;
import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.ability.DamageAttributionTracker;
import com.shipovskijkorp.scythes.mod.ability.ScytheAdvancementTracker;
import com.shipovskijkorp.scythes.mod.ability.ScytheCooldowns;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.entity.IceSpikeEntity;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

public final class FrozenScytheItem extends ScytheSwordItem {

    public FrozenScytheItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        if (!(entity instanceof Player player)) return;
        if (!isTheOnlyHeldStackToTick(player, stack)) return;
        freezeNearbyWater(player, level, ScytheBalance.Frozen.COLD_MASTER_FROST_WALKER_LEVEL);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.hurtEnemy(stack, target, attacker);
        if (ScytheCombatUtil.isProtectedWitheringMinion(attacker, target)) return;
        if (attacker.level().isClientSide()) return;
        if (target.isAlive() && attacker.getRandom().nextDouble() < ScytheBalance.Frozen.COLD_MASTER_FREEZING_CHANCE) {
            target.addEffect(new MobEffectInstance(ScytheMod.FREEZING, ScytheBalance.Frozen.COLD_MASTER_FREEZING_TICKS, ScytheBalance.Frozen.COLD_MASTER_FREEZING_AMPLIFIER));
            if (attacker instanceof ServerPlayer player) {
                DamageAttributionTracker.recordFreezing(target, player, ScytheBalance.Frozen.COLD_MASTER_FREEZING_TICKS);
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

        int radius = Math.min(ScytheBalance.Frozen.FROST_WALKER_MAX_RADIUS, ScytheBalance.Frozen.FROST_WALKER_BASE_RADIUS + frostLevel);
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

        int cooldownLeft = ScytheCooldowns.remaining(player, ScytheCooldowns.Skill.ICE_SPIKE);
        if (cooldownLeft > 0) {
            player.sendOverlayMessage(Component.translatable("message.scythes.ice_spike.cooldown", Math.max(1, (cooldownLeft + 19) / 20)));
            return InteractionResult.FAIL;
        }
        if (!hasEnoughDurability(stack, ScytheBalance.Frozen.ICE_SPIKE_DURABILITY_COST)) {
            player.sendOverlayMessage(Component.translatable("message.scythes.scythe_ability.no_durability"));
            return InteractionResult.FAIL;
        }

        IceSpikeEntity spike = new IceSpikeEntity(serverLevel, player, stack);
        spike.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, ScytheBalance.Frozen.ICE_SPIKE_SPEED, 0.0F);
        serverLevel.addFreshEntity(spike);

        stack.hurtAndBreak(ScytheBalance.Frozen.ICE_SPIKE_DURABILITY_COST, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        ScytheCooldowns.start(player, ScytheCooldowns.Skill.ICE_SPIKE, ScytheBalance.Frozen.ICE_SPIKE_COOLDOWN_TICKS);
        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F, 0.75F + player.getRandom().nextFloat() * 0.15F);
        return InteractionResult.SUCCESS;
    }

    public static boolean hasEnoughDurability(ItemStack stack, int cost) {
        if (cost <= 0) return true;
        if (!stack.isDamageableItem()) return false;
        return stack.getMaxDamage() - stack.getDamageValue() >= cost;
    }
}
