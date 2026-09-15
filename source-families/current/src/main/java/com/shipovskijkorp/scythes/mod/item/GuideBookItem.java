package com.shipovskijkorp.scythes.mod.item;

import java.util.Objects;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** Server-safe guide item. Client code supplies the actual screen opener during client initialization. */
public final class GuideBookItem extends Item {
    private static Runnable clientOpener = () -> {};

    public GuideBookItem(Properties properties) {
        super(properties);
    }

    public static void installClientOpener(Runnable opener) {
        clientOpener = Objects.requireNonNull(opener, "opener");
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            clientOpener.run();
        }
        return InteractionResult.SUCCESS;
    }
}
