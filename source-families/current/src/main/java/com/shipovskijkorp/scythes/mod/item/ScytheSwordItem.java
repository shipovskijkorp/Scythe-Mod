package com.shipovskijkorp.scythes.mod.item;

import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
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

    @FunctionalInterface
    public interface TooltipAppender {
        void append(ItemStack stack, Item.TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type);
    }

    private static TooltipAppender tooltips = (stack, context, displayComponent, textConsumer, type) -> {};

    public static void installTooltipAppender(TooltipAppender appender) {
        tooltips = Objects.requireNonNull(appender, "appender");
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        tooltips.append(stack, context, displayComponent, textConsumer, type);
    }
}
