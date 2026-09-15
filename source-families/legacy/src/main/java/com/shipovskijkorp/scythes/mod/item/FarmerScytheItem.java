package com.shipovskijkorp.scythes.mod.item;

import com.shipovskijkorp.scythes.mod.ability.FarmerHarvestAbility;
import com.shipovskijkorp.scythes.mod.ability.FarmerHarvestHandler;
import com.shipovskijkorp.scythes.mod.ability.FarmerTillingHandler;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.HoeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/** Netherite-hoe based utility scythe for farming. */
public class FarmerScytheItem extends HoeItem {
    public FarmerScytheItem(Settings settings) {
        super(ScytheMaterial.FARMER, (int) ScytheBalance.Farmer.HOE_ATTACK_DAMAGE_BASELINE, ScytheBalance.Farmer.HOE_ATTACK_SPEED_BASELINE, settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        PlayerEntity user = context.getPlayer();
        if (user != null && user.isSneaking()) {
            if (context.getWorld().isClient) return ActionResult.SUCCESS;
            if (user instanceof ServerPlayerEntity player) {
                return FarmerHarvestAbility.tryActivate(player, context.getHand()) ? ActionResult.SUCCESS : ActionResult.FAIL;
            }
            return ActionResult.PASS;
        }

        if (FarmerHarvestHandler.isMatureCrop(context.getWorld().getBlockState(context.getBlockPos()))) {
            if (context.getWorld().isClient) return ActionResult.SUCCESS;
            if (user instanceof ServerPlayerEntity player
                    && FarmerHarvestHandler.reapCrop(player, context.getStack(), context.getBlockPos(), false)) {
                return ActionResult.SUCCESS;
            }
        }
        ActionResult tillResult = super.useOnBlock(context);
        if (tillResult != ActionResult.PASS) {
            FarmerTillingHandler.expand(context, this::applyTillingActionSilently);
        }
        return tillResult;
    }

    private boolean applyTillingActionSilently(ItemUsageContext context) {
        var action = TILLING_ACTIONS.get(context.getWorld().getBlockState(context.getBlockPos()).getBlock());
        if (action == null || !action.getFirst().test(context)) return false;

        action.getSecond().accept(context);
        PlayerEntity player = context.getPlayer();
        if (player != null) {
            context.getStack().damage(1, player, p -> p.sendToolBreakStatus(context.getHand()));
        }
        return true;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!user.isSneaking()) return TypedActionResult.pass(stack);
        if (world.isClient) return TypedActionResult.success(stack);
        if (user instanceof ServerPlayerEntity player) {
            return FarmerHarvestAbility.tryActivate(player, hand)
                    ? TypedActionResult.success(stack)
                    : TypedActionResult.fail(stack);
        }
        return TypedActionResult.pass(stack);
    }

    @Nullable
    public static Hand getHeldFarmerScytheHand(PlayerEntity player) {
        if (player.getMainHandStack().getItem() instanceof FarmerScytheItem) return Hand.MAIN_HAND;
        if (player.getOffHandStack().getItem() instanceof FarmerScytheItem) return Hand.OFF_HAND;
        return null;
    }

    public static boolean hasEnoughDurability(ItemStack stack, int cost) {
        if (cost <= 0) return true;
        if (!stack.isDamageable()) return false;
        return stack.getMaxDamage() - stack.getDamage() >= cost;
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
