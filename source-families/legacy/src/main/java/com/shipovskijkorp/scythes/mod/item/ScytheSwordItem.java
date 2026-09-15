package com.shipovskijkorp.scythes.mod.item;

import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.text.Text;
import net.minecraft.world.World;

/** Single material/attribute setup for all legacy sword-like scythes. */
public class ScytheSwordItem extends SwordItem {
    public ScytheSwordItem(Settings settings) {
        super(ScytheMaterial.INSTANCE, ScytheBalance.Base.ATTACK_DAMAGE_BONUS, ScytheBalance.Base.ATTACK_SPEED_MODIFIER, settings);
    }

    @FunctionalInterface
    public interface TooltipAppender {
        void append(ItemStack stack, World world, List<Text> tooltip, TooltipContext context);
    }

    private static TooltipAppender tooltips = (stack, world, tooltip, context) -> {};

    public static void installTooltipAppender(TooltipAppender appender) {
        tooltips = Objects.requireNonNull(appender, "appender");
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        tooltips.append(stack, world, tooltip, context);
    }
}
