package com.shipovskijkorp.scythes.mod.mixin;

import com.shipovskijkorp.scythes.mod.ability.ScytheAdvancementTracker;
import com.shipovskijkorp.scythes.mod.item.FrozenScytheItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.PowderSnowBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PowderSnowBlock.class)
public abstract class PowderSnowBlockMixin {

    @Inject(method = "canEntityWalkOnPowderSnow", at = @At("HEAD"), cancellable = true)
    private static void scythes$allowFrozenScytheCarrierToWalk(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof Player player && FrozenScytheItem.isInPlayerInventory(player)) {
            if (player instanceof ServerPlayer serverPlayer) {
                ScytheAdvancementTracker.markFrozenPassive(serverPlayer);
            }
            cir.setReturnValue(true);
        }
    }
}
