package com.shipovskijkorp.scythes.mod.item;

import java.util.Objects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
//? if <1.21.11 {
import net.minecraft.util.TypedActionResult;
//? }
import net.minecraft.world.World;

/** Server-safe guide item. Client code supplies the actual screen opener during client initialization. */
public final class GuideBookItem extends Item {
    private static Runnable clientOpener = () -> {};

    public GuideBookItem(Settings settings) {
        super(settings);
    }

    public static void installClientOpener(Runnable opener) {
        clientOpener = Objects.requireNonNull(opener, "opener");
    }

//? if >=1.21.11 {
    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient()) {
            clientOpener.run();
        }
        return ActionResult.SUCCESS;
    }
//? } else {
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient()) {
            clientOpener.run();
        }
        return TypedActionResult.success(stack, world.isClient());
    }
//? }
}
