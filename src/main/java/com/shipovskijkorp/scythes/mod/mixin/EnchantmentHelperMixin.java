package com.shipovskijkorp.scythes.mod.mixin;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {

    @Inject(method = "getPossibleEntries", at = @At("RETURN"), cancellable = true)
    private static void scythes$restrictSpikedBladeToBloodScythe(int power, ItemStack stack, boolean treasureAllowed, CallbackInfoReturnable<List<EnchantmentLevelEntry>> cir) {
        if (stack.isOf(ScytheMod.BLOODY_SCYTHE)) {
            return;
        }

        List<EnchantmentLevelEntry> filtered = cir.getReturnValue().stream()
                .filter(entry -> entry.enchantment != ScytheMod.SPIKED_BLADE)
                .toList();

        cir.setReturnValue(filtered);
    }
}
