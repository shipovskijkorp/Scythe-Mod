package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.item.FarmerScytheItem;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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
import net.minecraft.world.phys.AABB;

/** Minecraft-facing crop operations shared by the Farmer Scythe abilities. */
public final class FarmerHarvestHandler {
    private FarmerHarvestHandler() {}

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
        if (!level.mayInteract(player, pos)) return false;

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
        int[] offsets = ScytheSphereOffsets.forRadius(ScytheBalance.Farmer.MASS_HARVEST_RADIUS);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int harvested = 0;

        for (int i = 0; i < offsets.length; i += 3) {
            pos.set(origin.getX() + offsets[i], origin.getY() + offsets[i + 1], origin.getZ() + offsets[i + 2]);
            if (isMatureCrop(level.getBlockState(pos)) && reapCrop(player, tool, pos, true)) harvested++;
        }
        return harvested;
    }

    public static int accelerateNearby(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos origin = player.blockPosition();
        int[] offsets = ScytheSphereOffsets.forRadius(ScytheBalance.Farmer.GROWTH_ACCELERATION_RADIUS);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int affected = 0;

        for (int i = 0; i < offsets.length; i += 3) {
            pos.set(origin.getX() + offsets[i], origin.getY() + offsets[i + 1], origin.getZ() + offsets[i + 2]);
            if (!level.mayInteract(player, pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (wasGrowthAccelerated(level, pos, state)) continue;
            BlockState accelerated = accelerateCrop(state);
            if (!accelerated.equals(state)) {
                level.setBlock(pos, accelerated, Block.UPDATE_CLIENTS);
                markGrowthAccelerated(level, pos, accelerated);
                affected++;
            }
        }
        return affected;
    }

    /**
     * Captures a normal player crop break before the vanilla/loader drop pipeline runs.
     * The actual bonus is duplicated from the item entities produced by that pipeline,
     * so loot modifiers and other mods are preserved instead of recomputing loot here.
     */
    public static void prepareCropBreak(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
        if (!isMatureCrop(state)) return;

        ItemStack tool = player.getMainHandItem();
        boolean doubleDrops = tool.getItem() instanceof FarmerScytheItem
                && player.getRandom().nextDouble() < ScytheBalance.Farmer.DOUBLE_DROP_CHANCE;
        Set<UUID> existingDrops = doubleDrops ? nearbyItemIds(level, pos) : Set.of();

        level.getServer().execute(() -> {
            // If the break was cancelled, the exact mature state is still present.
            if (level.getBlockState(pos).equals(state)) return;
            clearGrowthAcceleration(level, pos);
            if (doubleDrops) duplicateNewNearbyDrops(level, pos, existingDrops);
        });
    }

    /** NeoForge exposes the final mutable drop list directly, which is even safer than deferred entity capture. */
    public static void doubleFinalDrops(
            ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, List<ItemEntity> drops) {
        clearGrowthAcceleration(level, pos);
        ItemStack tool = player.getMainHandItem();
        if (!(tool.getItem() instanceof FarmerScytheItem) || !isMatureCrop(state)) return;
        if (player.getRandom().nextDouble() >= ScytheBalance.Farmer.DOUBLE_DROP_CHANCE) return;

        int originalSize = drops.size();
        for (int i = 0; i < originalSize; i++) {
            ItemEntity original = drops.get(i);
            ItemStack stack = original.getItem();
            if (stack.isEmpty()) continue;
            drops.add(new ItemEntity(level, original.getX(), original.getY(), original.getZ(), stack.copy()));
        }
    }

    private static Set<UUID> nearbyItemIds(ServerLevel level, BlockPos pos) {
        AABB box = new AABB(
                pos.getX() - 1.5D, pos.getY() - 1.5D, pos.getZ() - 1.5D,
                pos.getX() + 2.5D, pos.getY() + 2.5D, pos.getZ() + 2.5D);
        Set<UUID> ids = new HashSet<>();
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, box)) ids.add(item.getUUID());
        return ids;
    }

    private static void duplicateNewNearbyDrops(ServerLevel level, BlockPos pos, Set<UUID> existingDrops) {
        AABB box = new AABB(
                pos.getX() - 1.5D, pos.getY() - 1.5D, pos.getZ() - 1.5D,
                pos.getX() + 2.5D, pos.getY() + 2.5D, pos.getZ() + 2.5D);
        List<ItemEntity> produced = new ArrayList<>();
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, box)) {
            if (!existingDrops.contains(item.getUUID()) && !item.getItem().isEmpty()) produced.add(item);
        }
        for (ItemEntity item : produced) {
            level.addFreshEntity(new ItemEntity(level, item.getX(), item.getY(), item.getZ(), item.getItem().copy()));
        }
    }

    private static boolean wasGrowthAccelerated(ServerLevel level, BlockPos pos, BlockState state) {
        return ScythePersistentStore.isCropAccelerated(
                ScytheRuntimeState.worldRoot(level.getServer()), dimensionKey(level), pos.asLong(), cropKey(state), cropAge(state));
    }

    private static void markGrowthAccelerated(ServerLevel level, BlockPos pos, BlockState state) {
        ScythePersistentStore.markCropAccelerated(
                ScytheRuntimeState.worldRoot(level.getServer()), dimensionKey(level), pos.asLong(), cropKey(state), cropAge(state));
    }

    private static void clearGrowthAcceleration(ServerLevel level, BlockPos pos) {
        ScythePersistentStore.clearCropAcceleration(
                ScytheRuntimeState.worldRoot(level.getServer()), dimensionKey(level), pos.asLong());
    }

    private static String dimensionKey(ServerLevel level) {
        return level.dimension().identifier().toString();
    }

    private static String cropKey(BlockState state) {
        return state.getBlock().getDescriptionId();
    }

    private static int cropAge(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof CropBlock crop) return crop.getAge(state);
        if (block instanceof NetherWartBlock) return state.getValue(NetherWartBlock.AGE);
        if (block instanceof CocoaBlock) return state.getValue(CocoaBlock.AGE);
        if (block instanceof SweetBerryBushBlock) return state.getValue(SweetBerryBushBlock.AGE);
        return -1;
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
