package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.item.FarmerScytheItem;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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
import net.minecraft.util.math.Box;

/** Minecraft-facing crop operations shared by the Farmer Scythe abilities. */
public final class FarmerHarvestHandler {
    private static final int NETHER_WART_MAX_AGE = NetherWartBlock.AGE.getValues().stream()
            .mapToInt(Integer::intValue)
            .max()
            .orElse(3);

    private FarmerHarvestHandler() {}


    public static boolean isMatureCrop(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof CropBlock crop) {
            return crop.isMature(state);
        }
        if (block instanceof NetherWartBlock) {
            return state.get(NetherWartBlock.AGE) >= NETHER_WART_MAX_AGE;
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
        ServerWorld world = player.getServerWorld();
        BlockState state = world.getBlockState(pos);
        if (!isMatureCrop(state)) return false;
        if (!world.canPlayerModifyAt(player, pos)) return false;

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
        ServerWorld world = player.getServerWorld();
        BlockPos origin = player.getBlockPos();
        int[] offsets = ScytheSphereOffsets.forRadius(ScytheBalance.Farmer.MASS_HARVEST_RADIUS);
        BlockPos.Mutable pos = new BlockPos.Mutable();
        int harvested = 0;

        for (int i = 0; i < offsets.length; i += 3) {
            pos.set(origin.getX() + offsets[i], origin.getY() + offsets[i + 1], origin.getZ() + offsets[i + 2]);
            if (isMatureCrop(world.getBlockState(pos)) && reapCrop(player, tool, pos, true)) harvested++;
        }
        return harvested;
    }

    public static int accelerateNearby(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        BlockPos origin = player.getBlockPos();
        int[] offsets = ScytheSphereOffsets.forRadius(ScytheBalance.Farmer.GROWTH_ACCELERATION_RADIUS);
        BlockPos.Mutable pos = new BlockPos.Mutable();
        int affected = 0;

        for (int i = 0; i < offsets.length; i += 3) {
            pos.set(origin.getX() + offsets[i], origin.getY() + offsets[i + 1], origin.getZ() + offsets[i + 2]);
            if (!world.canPlayerModifyAt(player, pos)) continue;
            BlockState state = world.getBlockState(pos);
            if (wasGrowthAccelerated(world, pos, state)) continue;
            BlockState accelerated = accelerateCrop(state);
            if (!accelerated.equals(state)) {
                world.setBlockState(pos, accelerated, Block.NOTIFY_LISTENERS);
                markGrowthAccelerated(world, pos, accelerated);
                affected++;
            }
        }
        return affected;
    }

    /** Adds the passive bonus roll after a normally mined mature crop. */
    /**
     * Captures a normal player crop break before the vanilla/loader drop pipeline runs.
     * Bonus drops are copied from the actual spawned item entities, preserving loot
     * modifiers instead of calling Block.getDroppedStacks a second time.
     */
    public static void prepareCropBreak(ServerWorld world, ServerPlayerEntity player, BlockPos pos, BlockState state) {
        if (!isMatureCrop(state)) return;

        ItemStack tool = player.getMainHandStack();
        boolean doubleDrops = tool.getItem() instanceof FarmerScytheItem
                && player.getRandom().nextDouble() < ScytheBalance.Farmer.DOUBLE_DROP_CHANCE;
        Set<UUID> existingDrops = doubleDrops ? nearbyItemIds(world, pos) : Set.of();

        world.getServer().execute(() -> {
            if (world.getBlockState(pos).equals(state)) return;
            clearGrowthAcceleration(world, pos);
            if (doubleDrops) duplicateNewNearbyDrops(world, pos, existingDrops);
        });
    }

    private static Set<UUID> nearbyItemIds(ServerWorld world, BlockPos pos) {
        Box box = new Box(
                pos.getX() - 1.5D, pos.getY() - 1.5D, pos.getZ() - 1.5D,
                pos.getX() + 2.5D, pos.getY() + 2.5D, pos.getZ() + 2.5D);
        Set<UUID> ids = new HashSet<>();
        for (ItemEntity item : world.getEntitiesByClass(ItemEntity.class, box, item -> true)) ids.add(item.getUuid());
        return ids;
    }

    private static void duplicateNewNearbyDrops(ServerWorld world, BlockPos pos, Set<UUID> existingDrops) {
        Box box = new Box(
                pos.getX() - 1.5D, pos.getY() - 1.5D, pos.getZ() - 1.5D,
                pos.getX() + 2.5D, pos.getY() + 2.5D, pos.getZ() + 2.5D);
        List<ItemEntity> produced = new ArrayList<>();
        for (ItemEntity item : world.getEntitiesByClass(ItemEntity.class, box, item -> true)) {
            if (!existingDrops.contains(item.getUuid()) && !item.getStack().isEmpty()) produced.add(item);
        }
        for (ItemEntity item : produced) {
            world.spawnEntity(new ItemEntity(world, item.getX(), item.getY(), item.getZ(), item.getStack().copy()));
        }
    }

    private static boolean wasGrowthAccelerated(ServerWorld world, BlockPos pos, BlockState state) {
        return ScythePersistentStore.isCropAccelerated(
                ScytheRuntimeState.worldRoot(world.getServer()), dimensionKey(world), pos.asLong(), cropKey(state), cropAge(state));
    }

    private static void markGrowthAccelerated(ServerWorld world, BlockPos pos, BlockState state) {
        ScythePersistentStore.markCropAccelerated(
                ScytheRuntimeState.worldRoot(world.getServer()), dimensionKey(world), pos.asLong(), cropKey(state), cropAge(state));
    }

    private static void clearGrowthAcceleration(ServerWorld world, BlockPos pos) {
        ScythePersistentStore.clearCropAcceleration(
                ScytheRuntimeState.worldRoot(world.getServer()), dimensionKey(world), pos.asLong());
    }

    private static String dimensionKey(ServerWorld world) {
        return world.getRegistryKey().getValue().toString();
    }

    private static String cropKey(BlockState state) {
        return state.getBlock().getTranslationKey();
    }

    private static int cropAge(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof CropBlock crop) return crop.getAge(state);
        if (block instanceof NetherWartBlock) return state.get(NetherWartBlock.AGE);
        if (block instanceof CocoaBlock) return state.get(CocoaBlock.AGE);
        if (block instanceof SweetBerryBushBlock) return state.get(SweetBerryBushBlock.AGE);
        return -1;
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
            return age >= NETHER_WART_MAX_AGE ? state : state.with(NetherWartBlock.AGE, acceleratedAge(age, NETHER_WART_MAX_AGE));
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
}
