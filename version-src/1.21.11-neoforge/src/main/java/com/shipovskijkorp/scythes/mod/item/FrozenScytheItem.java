package com.shipovskijkorp.scythes.mod.item;

import com.shipovskijkorp.scythes.mod.util.ScytheCombatUtil;
import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.ability.DamageAttributionTracker;
import com.shipovskijkorp.scythes.mod.ability.ScytheAdvancementTracker;
import com.shipovskijkorp.scythes.mod.ability.ScytheCooldowns;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.entity.IceSpikeEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jspecify.annotations.Nullable;

public class FrozenScytheItem extends ScytheSwordItem {

    public FrozenScytheItem(Settings settings) {
        super(settings);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, world, entity, slot);

        if (!(entity instanceof PlayerEntity player)) return;
        if (!isTheOnlyHeldStackToTick(player, stack)) return;

        freezeNearbyWater(player, world, ScytheBalance.Frozen.COLD_MASTER_FROST_WALKER_LEVEL);
    }

    @Override
    public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.postHit(stack, target, attacker);
        if (ScytheCombatUtil.isProtectedWitheringMinion(attacker, target)) return;

        if (target.isAlive() && attacker.getRandom().nextDouble() < ScytheBalance.Frozen.COLD_MASTER_FREEZING_CHANCE) {
            target.addStatusEffect(new StatusEffectInstance(
                    ScytheMod.FREEZING,
                    ScytheBalance.Frozen.COLD_MASTER_FREEZING_TICKS,
                    ScytheBalance.Frozen.COLD_MASTER_FREEZING_AMPLIFIER
            ));
            if (attacker instanceof ServerPlayerEntity player) {
                DamageAttributionTracker.recordFreezing(target, player, ScytheBalance.Frozen.COLD_MASTER_FREEZING_TICKS);
                ScytheAdvancementTracker.markFrozenPassive(player);
            }
        }
    }

    public static boolean isInPlayerInventory(PlayerEntity player) {
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            if (player.getInventory().getStack(slot).getItem() instanceof FrozenScytheItem) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static Hand getHeldFrozenScytheHand(PlayerEntity player) {
        if (player.getMainHandStack().getItem() instanceof FrozenScytheItem) {
            return Hand.MAIN_HAND;
        }
        if (player.getOffHandStack().getItem() instanceof FrozenScytheItem) {
            return Hand.OFF_HAND;
        }
        return null;
    }

    private static boolean isTheOnlyHeldStackToTick(PlayerEntity player, ItemStack stack) {
        if (player.getMainHandStack() == stack) return true;
        return !(player.getMainHandStack().getItem() instanceof FrozenScytheItem)
                && player.getOffHandStack() == stack;
    }

    private static void freezeNearbyWater(PlayerEntity player, ServerWorld world, int level) {
        // Match Frost Walker's location requirements: it only works while standing
        // on the ground and not while riding another entity.
        if (!player.isOnGround() || player.hasVehicle() || level <= 0) return;

        int radius = Math.min(ScytheBalance.Frozen.FROST_WALKER_MAX_RADIUS, ScytheBalance.Frozen.FROST_WALKER_BASE_RADIUS + level);
        double radiusSquared = radius * radius;
        BlockPos center = player.getBlockPos();
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        BlockState frostedIce = Blocks.FROSTED_ICE.getDefaultState();

        for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
            for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                mutable.set(x, center.getY() - 1, z);
                BlockPos waterPos = mutable.toImmutable();

                double dx = waterPos.getX() + 0.5D - player.getX();
                double dy = waterPos.getY() + 0.5D - player.getY();
                double dz = waterPos.getZ() + 0.5D - player.getZ();
                if (dx * dx + dy * dy + dz * dz >= radiusSquared) continue;

                BlockState waterState = world.getBlockState(waterPos);
                FluidState fluid = waterState.getFluidState();
                if (!world.getBlockState(waterPos.up()).isAir()) continue;
                if (!waterState.isOf(Blocks.WATER)) continue;
                if (!fluid.isIn(FluidTags.WATER) || !fluid.isStill()) continue;
                if (!world.doesNotIntersectEntities(null, VoxelShapes.fullCube().offset(waterPos))) continue;

                // FrostedIceBlock.onBlockAdded schedules the melting tick itself;
                // scheduling another tick here made ported ice melt too quickly.
                if (world.setBlockState(waterPos, frostedIce)) {
                    world.emitGameEvent(player, GameEvent.BLOCK_PLACE, waterPos);
                }
            }
        }
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!(world instanceof ServerWorld serverWorld)) {
            return ActionResult.SUCCESS;
        }

        if (!(user instanceof ServerPlayerEntity player)) {
            return ActionResult.PASS;
        }

        int cooldownLeft = ScytheCooldowns.remaining(player, ScytheCooldowns.Skill.ICE_SPIKE);
        if (cooldownLeft > 0) {
            player.sendMessage(
                    Text.translatable("message.scythes.ice_spike.cooldown", Math.max(1, (cooldownLeft + 19) / 20)),
                    true
            );
            return ActionResult.FAIL;
        }

        if (!hasEnoughDurability(stack, ScytheBalance.Frozen.ICE_SPIKE_DURABILITY_COST)) {
            player.sendMessage(Text.translatable("message.scythes.scythe_ability.no_durability"), true);
            return ActionResult.FAIL;
        }

        IceSpikeEntity spike = new IceSpikeEntity(serverWorld, player, stack);
        spike.refreshPositionAndAngles(player.getX(), player.getEyeY() - 0.1D, player.getZ(), player.getYaw(), player.getPitch());
        spike.setVelocity(player, player.getPitch(), player.getYaw(), 0.0F, ScytheBalance.Frozen.ICE_SPIKE_SPEED, 0.0F);
        serverWorld.spawnEntity(spike);

        stack.damage(
                ScytheBalance.Frozen.ICE_SPIKE_DURABILITY_COST,
                player,
                hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND
        );
        ScytheCooldowns.start(player, ScytheCooldowns.Skill.ICE_SPIKE, ScytheBalance.Frozen.ICE_SPIKE_COOLDOWN_TICKS);

        serverWorld.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ENTITY_ARROW_SHOOT,
                SoundCategory.PLAYERS,
                1.0F,
                0.75F + player.getRandom().nextFloat() * 0.15F
        );

        return ActionResult.SUCCESS;
    }

    public static boolean hasEnoughDurability(ItemStack stack, int cost) {
        if (cost <= 0) return true;
        if (!stack.isDamageable()) return false;
        return stack.getMaxDamage() - stack.getDamage() >= cost;
    }
}
