package com.example.bloodyscythe.mixin;

import com.example.bloodyscythe.BleedingMod;
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
    private static void bloodyscythe$restrictSpikedBladeToBloodScythe(int power, ItemStack stack, boolean treasureAllowed, CallbackInfoReturnable<List<EnchantmentLevelEntry>> cir) {
        if (stack.isOf(BleedingMod.BLOODY_SCYTHE)) {
            return;
        }

        List<EnchantmentLevelEntry> filtered = cir.getReturnValue().stream()
                .filter(entry -> entry.enchantment != BleedingMod.SPIKED_BLADE)
                .toList();

        cir.setReturnValue(filtered);
    }
}
