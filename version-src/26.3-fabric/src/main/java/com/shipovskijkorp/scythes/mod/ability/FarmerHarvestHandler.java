package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.item.FarmerScytheItem;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Minecraft-facing crop operations shared by the Farmer Scythe abilities. */
public final class FarmerHarvestHandler {
    private FarmerHarvestHandler() {}

    /** One activation per planted crop. Entries disappear with the world and are cleared on harvest/break. */
    private static final Map<ServerLevel, Set<Long>> ACCELERATED_CROPS = new WeakHashMap<>();

    public static boolean isMatureCrop(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof CropBlock crop) {
            return crop.isMaxAge(state);
        }
        if (block instanceof NetherWartBlock) {
            return state.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE;
        }
        if (block instanceof CocoaBlock) {
            return state.getValue(CocoaBlock.AGE) >= CocoaBlock.MAX_AGE;
        }
        if (block instanceof SweetBerryBushBlock) {
            return state.getValue(SweetBerryBushBlock.AGE) >= SweetBerryBushBlock.MAX_AGE;
        }
        return false;
    }

    public static boolean reapCrop(ServerPlayer player, ItemStack tool, BlockPos pos, boolean intoInventory) {
        ServerLevel level = (ServerLevel) player.level();
        BlockState state = level.getBlockState(pos);
        if (!isMatureCrop(state)) return false;

        List<ItemStack> drops = new ArrayList<>(Block.getDrops(state, level, pos, null, player, tool));
        if (player.getRandom().nextDouble() < ScytheBalance.Farmer.DOUBLE_DROP_CHANCE) {
            int originalSize = drops.size();
            for (int i = 0; i < originalSize; i++) {
                drops.add(drops.get(i).copy());
            }
        }

        clearGrowthAcceleration(level, pos);
        level.setBlock(pos, resetCrop(state), Block.UPDATE_ALL);
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) continue;
            if (intoInventory) {
                ItemStack remainder = drop.copy();
                player.getInventory().add(remainder);
                if (!remainder.isEmpty()) {
                    player.drop(remainder, false, Prediction.SERVER_ONLY);
                }
            } else {
                level.addFreshEntity(new ItemEntity(
                        level,
                        pos.getX() + 0.5D,
                        pos.getY() + 0.5D,
                        pos.getZ() + 0.5D,
                        drop.copy()
                ));
            }
        }
        return true;
    }

    public static int massHarvest(ServerPlayer player, ItemStack tool) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos origin = player.blockPosition();
        int radius = (int) Math.ceil(ScytheBalance.Farmer.MASS_HARVEST_RADIUS);
        double radiusSquared = ScytheBalance.Farmer.MASS_HARVEST_RADIUS * ScytheBalance.Farmer.MASS_HARVEST_RADIUS;
        int harvested = 0;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if ((double) dx * dx + (double) dy * dy + (double) dz * dz > radiusSquared) continue;
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (isMatureCrop(level.getBlockState(pos)) && reapCrop(player, tool, pos, true)) {
                        harvested++;
                    }
                }
            }
        }
        return harvested;
    }

    public static int accelerateNearby(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos origin = player.blockPosition();
        int radius = (int) Math.ceil(ScytheBalance.Farmer.GROWTH_ACCELERATION_RADIUS);
        double radiusSquared = ScytheBalance.Farmer.GROWTH_ACCELERATION_RADIUS * ScytheBalance.Farmer.GROWTH_ACCELERATION_RADIUS;
        int affected = 0;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if ((double) dx * dx + (double) dy * dy + (double) dz * dz > radiusSquared) continue;
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (wasGrowthAccelerated(level, pos)) continue;
                    BlockState state = level.getBlockState(pos);
                    BlockState accelerated = accelerateCrop(state);
                    if (!accelerated.equals(state)) {
                        level.setBlock(pos, accelerated, Block.UPDATE_CLIENTS);
                        markGrowthAccelerated(level, pos);
                        affected++;
                    }
                }
            }
        }
        return affected;
    }

    /** Adds the passive bonus roll after a normally mined mature crop. */
    public static void afterCropBroken(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
        clearGrowthAcceleration(level, pos);
        ItemStack tool = player.getMainHandItem();
        if (!(tool.getItem() instanceof FarmerScytheItem)) return;
        if (!isMatureCrop(state)) return;
        if (player.getRandom().nextDouble() >= ScytheBalance.Farmer.DOUBLE_DROP_CHANCE) return;

        for (ItemStack drop : Block.getDrops(state, level, pos, null, player, tool)) {
            if (!drop.isEmpty()) {
                level.addFreshEntity(new ItemEntity(
                        level,
                        pos.getX() + 0.5D,
                        pos.getY() + 0.5D,
                        pos.getZ() + 0.5D,
                        drop.copy()
                ));
            }
        }
    }

    private static boolean wasGrowthAccelerated(ServerLevel level, BlockPos pos) {
        Set<Long> positions = ACCELERATED_CROPS.get(level);
        return positions != null && positions.contains(pos.asLong());
    }

    private static void markGrowthAccelerated(ServerLevel level, BlockPos pos) {
        ACCELERATED_CROPS.computeIfAbsent(level, ignored -> new HashSet<>()).add(pos.asLong());
    }

    private static void clearGrowthAcceleration(ServerLevel level, BlockPos pos) {
        Set<Long> positions = ACCELERATED_CROPS.get(level);
        if (positions != null) positions.remove(pos.asLong());
    }

    public static boolean hasBoneMeal(ServerPlayer player, int amount) {
        if (player.getAbilities().instabuild) return true;
        int found = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(Items.BONE_MEAL)) {
                found += stack.getCount();
                if (found >= amount) return true;
            }
        }
        return false;
    }

    public static void consumeBoneMeal(ServerPlayer player, int amount) {
        if (player.getAbilities().instabuild) return;
        int left = amount;
        for (int i = 0; i < player.getInventory().getContainerSize() && left > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.is(Items.BONE_MEAL)) continue;
            int take = Math.min(left, stack.getCount());
            stack.shrink(take);
            left -= take;
        }
    }

    private static BlockState resetCrop(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof CropBlock crop) {
            return crop.getStateForAge(0);
        }
        if (block instanceof NetherWartBlock) {
            return state.setValue(NetherWartBlock.AGE, 0);
        }
        if (block instanceof CocoaBlock) {
            return state.setValue(CocoaBlock.AGE, 0);
        }
        if (block instanceof SweetBerryBushBlock) {
            return state.setValue(SweetBerryBushBlock.AGE, 0);
        }
        return state;
    }

    private static BlockState accelerateCrop(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof CropBlock crop) {
            int age = crop.getAge(state);
            int max = crop.getMaxAge();
            return age >= max ? state : crop.getStateForAge(acceleratedAge(age, max));
        }
        if (block instanceof NetherWartBlock) {
            int age = state.getValue(NetherWartBlock.AGE);
            return age >= NetherWartBlock.MAX_AGE ? state : state.setValue(NetherWartBlock.AGE, acceleratedAge(age, NetherWartBlock.MAX_AGE));
        }
        if (block instanceof CocoaBlock) {
            int age = state.getValue(CocoaBlock.AGE);
            return age >= CocoaBlock.MAX_AGE ? state : state.setValue(CocoaBlock.AGE, acceleratedAge(age, CocoaBlock.MAX_AGE));
        }
        if (block instanceof SweetBerryBushBlock) {
            int age = state.getValue(SweetBerryBushBlock.AGE);
            return age >= SweetBerryBushBlock.MAX_AGE ? state : state.setValue(SweetBerryBushBlock.AGE, acceleratedAge(age, SweetBerryBushBlock.MAX_AGE));
        }
        return state;
    }

    private static int acceleratedAge(int age, int maxAge) {
        int remaining = maxAge - age;
        int advance = Math.max(1, (int) Math.ceil(remaining * ScytheBalance.Farmer.GROWTH_REDUCTION_FRACTION));
        return Math.min(maxAge, age + advance);
    }
}
