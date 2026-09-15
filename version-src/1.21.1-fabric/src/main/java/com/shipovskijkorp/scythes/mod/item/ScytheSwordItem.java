package com.shipovskijkorp.scythes.mod.item;

import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import java.util.List;
import java.util.Objects;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;

/** Single material/attribute setup for all legacy sword-like scythes. */
public class ScytheSwordItem extends SwordItem {
    public ScytheSwordItem(Settings settings) {
        super(ScytheMaterial.INSTANCE, settings.attributeModifiers(SwordItem.createAttributeModifiers(ScytheMaterial.INSTANCE, ScytheBalance.Base.ATTACK_DAMAGE_BONUS, ScytheBalance.Base.ATTACK_SPEED_MODIFIER)));
    }

    @FunctionalInterface
    public interface TooltipAppender {
        void append(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type);
    }

    private static TooltipAppender tooltips = (stack, context, tooltip, type) -> {};

    public static void installTooltipAppender(TooltipAppender appender) {
        tooltips = Objects.requireNonNull(appender, "appender");
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltips.append(stack, context, tooltip, type);
    }
}
