package com.shipovskijkorp.scythes.mod.mixin;

import com.shipovskijkorp.scythes.mod.ability.ScytheAdvancementTracker;
import com.shipovskijkorp.scythes.mod.item.FrozenScytheItem;
import net.minecraft.block.PowderSnowBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PowderSnowBlock.class)
public abstract class PowderSnowBlockMixin {

    @Inject(method = "canWalkOnPowderSnow", at = @At("HEAD"), cancellable = true)
    private static void scythes$allowFrozenScytheCarrierToWalk(
            Entity entity,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (entity instanceof PlayerEntity player && FrozenScytheItem.isInPlayerInventory(player)) {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                ScytheAdvancementTracker.markFrozenPassive(serverPlayer);
            }
            cir.setReturnValue(true);
        }
    }
}
