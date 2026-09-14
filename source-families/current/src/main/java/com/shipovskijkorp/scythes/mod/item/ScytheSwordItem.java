package com.shipovskijkorp.scythes.mod.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Common sword-like base for all scythes.
 *
 * Minecraft 26.1+ moved most vanilla sword data to Item.Properties#sword(...),
 * but a few behaviours and third-party checks are still easier to preserve from
 * an item class that is explicitly weapon-like. Keeping this base class also
 * prevents individual scythes from drifting apart when sword behaviour changes.
 */
public class ScytheSwordItem extends Item {

    public ScytheSwordItem(Properties settings) {
        super(settings);
    }


    /**
     * Vanilla swords do not let creative players attack blocks with them.
     * Kept without @Override so this remains source-compatible if the exact
     * method is moved/renamed again in later experimental mappings.
     */
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return !player.isCreative();
    }
}
