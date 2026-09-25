package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/** Expands a successful vanilla hoe action into the Farmer Scythe's hydrated 3x3 tilling passive. */
public final class FarmerTillingHandler {
    private FarmerTillingHandler() {}

    @FunctionalInterface
    public interface TillingAction {
        boolean apply(ItemUsageContext context);
    }

    public static void expand(ItemUsageContext origin, TillingAction tillingAction) {
        if (!(origin.getWorld() instanceof ServerWorld world)) return;

        BlockPos center = origin.getBlockPos();
        hydrateFarmland(world, center);

        PlayerEntity player = origin.getPlayer();
        if (player == null || origin.getStack().isEmpty()) return;
        if (player instanceof ServerPlayerEntity serverPlayer) ScytheAdvancementTracker.markFarmerPassive(serverPlayer);

        int radius = ScytheBalance.Farmer.TILLING_RADIUS;
        outer:
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx == 0 && dz == 0) continue;
                if (origin.getStack().isEmpty()) break outer;

                BlockPos pos = center.add(dx, 0, dz);
                if (player instanceof ServerPlayerEntity serverPlayer) {
//? if >=1.21.11 {
                    if (!serverPlayer.canModifyAt(world, pos)) continue;
//? } else {
                    if (!world.canPlayerModifyAt(serverPlayer, pos)) continue;
//? }
                }
                ItemUsageContext areaContext = new ItemUsageContext(
                        player,
                        origin.getHand(),
                        new BlockHitResult(
                                new Vec3d(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D),
                                origin.getSide(),
                                pos,
                                origin.hitsInsideBlock()
                        )
                );
                if (tillingAction.apply(areaContext)) {
                    hydrateFarmland(world, pos);
                }
            }
        }
    }

    private static void hydrateFarmland(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof FarmlandBlock)) return;
        if (state.get(FarmlandBlock.MOISTURE) >= FarmlandBlock.MAX_MOISTURE) return;
        world.setBlockState(
                pos,
                state.with(FarmlandBlock.MOISTURE, FarmlandBlock.MAX_MOISTURE),
                Block.NOTIFY_ALL
        );
    }
}
