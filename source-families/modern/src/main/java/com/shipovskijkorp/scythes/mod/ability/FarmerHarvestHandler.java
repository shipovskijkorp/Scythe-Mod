package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.item.FarmerScytheItem;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CocoaBlock;
import net.minecraft.block.CropBlock;
import net.minecraft.block.NetherWartBlock;
import net.minecraft.block.SweetBerryBushBlock;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/** Minecraft-facing crop operations shared by the Farmer Scythe abilities. */
public final class FarmerHarvestHandler {
    private FarmerHarvestHandler() {}

    /** One activation per planted crop. Entries disappear with the world and are cleared on harvest/break. */
    private static final Map<ServerWorld, Set<Long>> ACCELERATED_CROPS = new WeakHashMap<>();

    public static boolean isMatureCrop(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof CropBlock crop) {
            return crop.isMature(state);
        }
        if (block instanceof NetherWartBlock) {
            return state.get(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE;
        }
        if (block instanceof CocoaBlock) {
            return state.get(CocoaBlock.AGE) >= CocoaBlock.MAX_AGE;
        }
        if (block instanceof SweetBerryBushBlock) {
            return state.get(SweetBerryBushBlock.AGE) >= SweetBerryBushBlock.MAX_AGE;
        }
        return false;
    }

    public static boolean reapCrop(ServerPlayerEntity player, ItemStack tool, BlockPos pos, boolean intoInventory) {
        ServerWorld world = serverWorld(player);
        BlockState state = world.getBlockState(pos);
        if (!isMatureCrop(state)) return false;

        List<ItemStack> drops = new ArrayList<>(Block.getDroppedStacks(state, world, pos, null, player, tool));
        if (player.getRandom().nextDouble() < ScytheBalance.Farmer.DOUBLE_DROP_CHANCE) {
            int originalSize = drops.size();
            for (int i = 0; i < originalSize; i++) {
                drops.add(drops.get(i).copy());
            }
        }

        clearGrowthAcceleration(world, pos);
        world.setBlockState(pos, resetCrop(state), Block.NOTIFY_ALL);
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) continue;
            if (intoInventory) {
                ItemStack remainder = drop.copy();
                player.getInventory().insertStack(remainder);
                if (!remainder.isEmpty()) {
                    player.dropItem(remainder, false);
                }
            } else {
                world.spawnEntity(new ItemEntity(
                        world,
                        pos.getX() + 0.5D,
                        pos.getY() + 0.5D,
                        pos.getZ() + 0.5D,
                        drop.copy()
                ));
            }
        }
        return true;
    }

    public static int massHarvest(ServerPlayerEntity player, ItemStack tool) {
        ServerWorld world = serverWorld(player);
        BlockPos origin = player.getBlockPos();
        int radius = (int) Math.ceil(ScytheBalance.Farmer.MASS_HARVEST_RADIUS);
        double radiusSquared = ScytheBalance.Farmer.MASS_HARVEST_RADIUS * ScytheBalance.Farmer.MASS_HARVEST_RADIUS;
        int harvested = 0;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if ((double) dx * dx + (double) dy * dy + (double) dz * dz > radiusSquared) continue;
                    BlockPos pos = origin.add(dx, dy, dz);
                    if (isMatureCrop(world.getBlockState(pos)) && reapCrop(player, tool, pos, true)) {
                        harvested++;
                    }
                }
            }
        }
        return harvested;
    }

    public static int accelerateNearby(ServerPlayerEntity player) {
        ServerWorld world = serverWorld(player);
        BlockPos origin = player.getBlockPos();
        int radius = (int) Math.ceil(ScytheBalance.Farmer.GROWTH_ACCELERATION_RADIUS);
        double radiusSquared = ScytheBalance.Farmer.GROWTH_ACCELERATION_RADIUS * ScytheBalance.Farmer.GROWTH_ACCELERATION_RADIUS;
        int affected = 0;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if ((double) dx * dx + (double) dy * dy + (double) dz * dz > radiusSquared) continue;
                    BlockPos pos = origin.add(dx, dy, dz);
                    if (wasGrowthAccelerated(world, pos)) continue;
                    BlockState state = world.getBlockState(pos);
                    BlockState accelerated = accelerateCrop(state);
                    if (!accelerated.equals(state)) {
                        world.setBlockState(pos, accelerated, Block.NOTIFY_LISTENERS);
                        markGrowthAccelerated(world, pos);
                        affected++;
                    }
                }
            }
        }
        return affected;
    }

    public static void afterCropBroken(ServerWorld world, ServerPlayerEntity player, BlockPos pos, BlockState state) {
        clearGrowthAcceleration(world, pos);
        ItemStack tool = player.getMainHandStack();
        if (!(tool.getItem() instanceof FarmerScytheItem)) return;
        if (!isMatureCrop(state)) return;
        if (player.getRandom().nextDouble() >= ScytheBalance.Farmer.DOUBLE_DROP_CHANCE) return;

        for (ItemStack drop : Block.getDroppedStacks(state, world, pos, null, player, tool)) {
            if (!drop.isEmpty()) {
                world.spawnEntity(new ItemEntity(
                        world,
                        pos.getX() + 0.5D,
                        pos.getY() + 0.5D,
                        pos.getZ() + 0.5D,
                        drop.copy()
                ));
            }
        }
    }

    private static boolean wasGrowthAccelerated(ServerWorld world, BlockPos pos) {
        Set<Long> positions = ACCELERATED_CROPS.get(world);
        return positions != null && positions.contains(pos.asLong());
    }

    private static void markGrowthAccelerated(ServerWorld world, BlockPos pos) {
        ACCELERATED_CROPS.computeIfAbsent(world, ignored -> new HashSet<>()).add(pos.asLong());
    }

    private static void clearGrowthAcceleration(ServerWorld world, BlockPos pos) {
        Set<Long> positions = ACCELERATED_CROPS.get(world);
        if (positions != null) positions.remove(pos.asLong());
    }

    public static boolean hasBoneMeal(ServerPlayerEntity player, int amount) {
        if (player.getAbilities().creativeMode) return true;
        int found = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(Items.BONE_MEAL)) {
                found += stack.getCount();
                if (found >= amount) return true;
            }
        }
        return false;
    }

    public static void consumeBoneMeal(ServerPlayerEntity player, int amount) {
        if (player.getAbilities().creativeMode) return;
        int left = amount;
        for (int i = 0; i < player.getInventory().size() && left > 0; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isOf(Items.BONE_MEAL)) continue;
            int take = Math.min(left, stack.getCount());
            stack.decrement(take);
            left -= take;
        }
    }

    private static BlockState resetCrop(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof CropBlock crop) {
            return crop.withAge(0);
        }
        if (block instanceof NetherWartBlock) {
            return state.with(NetherWartBlock.AGE, 0);
        }
        if (block instanceof CocoaBlock) {
            return state.with(CocoaBlock.AGE, 0);
        }
        if (block instanceof SweetBerryBushBlock) {
            return state.with(SweetBerryBushBlock.AGE, 0);
        }
        return state;
    }

    private static BlockState accelerateCrop(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof CropBlock crop) {
            int age = crop.getAge(state);
            int max = crop.getMaxAge();
            return age >= max ? state : crop.withAge(acceleratedAge(age, max));
        }
        if (block instanceof NetherWartBlock) {
            int age = state.get(NetherWartBlock.AGE);
            return age >= NetherWartBlock.MAX_AGE ? state : state.with(NetherWartBlock.AGE, acceleratedAge(age, NetherWartBlock.MAX_AGE));
        }
        if (block instanceof CocoaBlock) {
            int age = state.get(CocoaBlock.AGE);
            return age >= CocoaBlock.MAX_AGE ? state : state.with(CocoaBlock.AGE, acceleratedAge(age, CocoaBlock.MAX_AGE));
        }
        if (block instanceof SweetBerryBushBlock) {
            int age = state.get(SweetBerryBushBlock.AGE);
            return age >= SweetBerryBushBlock.MAX_AGE ? state : state.with(SweetBerryBushBlock.AGE, acceleratedAge(age, SweetBerryBushBlock.MAX_AGE));
        }
        return state;
    }

    private static int acceleratedAge(int age, int maxAge) {
        int remaining = maxAge - age;
        int advance = Math.max(1, (int) Math.ceil(remaining * ScytheBalance.Farmer.GROWTH_REDUCTION_FRACTION));
        return Math.min(maxAge, age + advance);
    }

    private static ServerWorld serverWorld(ServerPlayerEntity player) {
//? if >=1.21.11 {
        return player.getEntityWorld();
//? } else {
        return player.getServerWorld();
//? }
    }
}
