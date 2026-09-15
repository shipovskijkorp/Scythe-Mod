package com.shipovskijkorp.scythes.mod.item;

import com.shipovskijkorp.scythes.mod.ability.FarmerHarvestAbility;
import com.shipovskijkorp.scythes.mod.ability.FarmerHarvestHandler;
import com.shipovskijkorp.scythes.mod.ability.FarmerTillingHandler;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Netherite-hoe based utility scythe for farming. */
public class FarmerScytheItem extends HoeItem {
    public FarmerScytheItem(Properties settings) {
        super(
                ScytheMaterial.FARMER,
                ScytheBalance.Farmer.HOE_ATTACK_DAMAGE_BASELINE,
                ScytheBalance.Farmer.HOE_ATTACK_SPEED_BASELINE,
                settings
        );
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player user = context.getPlayer();
        if (user != null && user.isShiftKeyDown()) {
            if (!(context.getLevel() instanceof ServerLevel)) return InteractionResult.SUCCESS;
            if (user instanceof ServerPlayer player) {
                return FarmerHarvestAbility.tryActivate(player, context.getHand()) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        }

        BlockPos pos = context.getClickedPos();
        if (FarmerHarvestHandler.isMatureCrop(context.getLevel().getBlockState(pos))) {
            if (!(context.getLevel() instanceof ServerLevel)) return InteractionResult.SUCCESS;
            if (user instanceof ServerPlayer player
                    && FarmerHarvestHandler.reapCrop(player, context.getItemInHand(), pos, false)) {
                return InteractionResult.SUCCESS;
            }
        }
        InteractionResult tillResult = super.useOn(context);
        if (tillResult != InteractionResult.PASS) {
            FarmerTillingHandler.expand(context, this::applyTillingActionSilently);
        }
        return tillResult;
    }

    private boolean applyTillingActionSilently(UseOnContext context) {
        var action = TILLABLES.get(context.getLevel().getBlockState(context.getClickedPos()).getBlock());
        if (action == null || !action.getFirst().test(context)) return false;

        action.getSecond().accept(context);
        Player player = context.getPlayer();
        if (player != null) {
            context.getItemInHand().hurtAndBreak(
                    1,
                    player,
                    context.getHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND
            );
        }
        return true;
    }

    @Override
    public InteractionResult use(Level level, Player user, InteractionHand hand) {
        if (!user.isShiftKeyDown()) return InteractionResult.PASS;
        if (!(level instanceof ServerLevel)) return InteractionResult.SUCCESS;
        if (user instanceof ServerPlayer player) {
            return FarmerHarvestAbility.tryActivate(player, hand) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    @Nullable
    public static InteractionHand getHeldFarmerScytheHand(Player player) {
        if (player.getMainHandItem().getItem() instanceof FarmerScytheItem) return InteractionHand.MAIN_HAND;
        if (player.getOffhandItem().getItem() instanceof FarmerScytheItem) return InteractionHand.OFF_HAND;
        return null;
    }

    public static boolean hasEnoughDurability(ItemStack stack, int cost) {
        if (cost <= 0) return true;
        if (!stack.isDamageableItem()) return false;
        return stack.getMaxDamage() - stack.getDamageValue() >= cost;
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
