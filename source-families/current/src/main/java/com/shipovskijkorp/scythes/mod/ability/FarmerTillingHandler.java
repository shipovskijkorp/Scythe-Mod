package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Expands a successful vanilla hoe action into the Farmer Scythe's hydrated 3x3 tilling passive. */
public final class FarmerTillingHandler {
    private FarmerTillingHandler() {}

    @FunctionalInterface
    public interface TillingAction {
        boolean apply(UseOnContext context);
    }

    public static void expand(UseOnContext origin, TillingAction tillingAction) {
        if (!(origin.getLevel() instanceof ServerLevel level)) return;

        BlockPos center = origin.getClickedPos();
        hydrateFarmland(level, center);

        Player player = origin.getPlayer();
        if (player == null || origin.getItemInHand().isEmpty()) return;
        if (player instanceof ServerPlayer serverPlayer) ScytheAdvancementTracker.markFarmerPassive(serverPlayer);

        int radius = ScytheBalance.Farmer.TILLING_RADIUS;
        outer:
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx == 0 && dz == 0) continue;
                if (origin.getItemInHand().isEmpty()) break outer;

                BlockPos pos = center.offset(dx, 0, dz);
                UseOnContext areaContext = new UseOnContext(
                        player,
                        origin.getHand(),
                        new BlockHitResult(
                                new Vec3(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D),
                                origin.getClickedFace(),
                                pos,
                                origin.isInside()
                        )
                );
                if (tillingAction.apply(areaContext)) {
                    hydrateFarmland(level, pos);
                }
            }
        }
    }

    private static void hydrateFarmland(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof FarmlandBlock)) return;
        if (state.getValue(FarmlandBlock.MOISTURE) >= FarmlandBlock.MAX_MOISTURE) return;
        level.setBlock(
                pos,
                state.setValue(FarmlandBlock.MOISTURE, FarmlandBlock.MAX_MOISTURE),
                Block.UPDATE_ALL
        );
    }
}
