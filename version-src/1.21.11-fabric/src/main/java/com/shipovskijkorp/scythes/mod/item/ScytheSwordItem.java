package com.shipovskijkorp.scythes.mod.item;

import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;

public class ScytheSwordItem extends Item {
    public ScytheSwordItem(Settings settings) { super(settings); }

    @FunctionalInterface
    public interface TooltipAppender {
        void append(ItemStack stack, Item.TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type);
    }

    private static TooltipAppender tooltips = (stack, context, displayComponent, textConsumer, type) -> {};

    public static void installTooltipAppender(TooltipAppender appender) {
        tooltips = Objects.requireNonNull(appender, "appender");
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        tooltips.append(stack, context, displayComponent, textConsumer, type);
    }
}
