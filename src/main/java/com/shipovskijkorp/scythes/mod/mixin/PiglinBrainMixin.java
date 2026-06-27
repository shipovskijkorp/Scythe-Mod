package com.shipovskijkorp.scythes.mod.mixin;

import com.shipovskijkorp.scythes.mod.item.GoldenScytheItem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.PiglinBrain;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PiglinBrain.class)
public abstract class PiglinBrainMixin {

    @Inject(method = "wearsGoldArmor", at = @At("HEAD"), cancellable = true)
    private static void scythes$goldenScytheCountsAsGold(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof PlayerEntity player && GoldenScytheItem.hasGoldenScythe(player)) {
            cir.setReturnValue(true);
        }
    }
}
