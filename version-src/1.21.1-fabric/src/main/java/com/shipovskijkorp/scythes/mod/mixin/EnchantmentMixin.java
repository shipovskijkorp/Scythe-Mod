package com.shipovskijkorp.scythes.mod.mixin;

import com.shipovskijkorp.scythes.mod.item.ScytheItemUtil;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Enchantment.class)
public abstract class EnchantmentMixin {

    @Inject(method = "isSupportedItem", at = @At("RETURN"), cancellable = true)
    private void scythes$acceptSwordEnchantmentsOnScythes(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        scythes$acceptSwordEnchantment(stack, cir);
    }

    @Inject(method = "isPrimaryItem", at = @At("RETURN"), cancellable = true)
    private void scythes$makeSwordEnchantmentsPrimaryOnScythes(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        scythes$acceptSwordEnchantment(stack, cir);
    }

    @Unique
    private void scythes$acceptSwordEnchantment(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() || !ScytheItemUtil.isScythe(stack)) {
            return;
        }

        Enchantment enchantment = (Enchantment) (Object) this;
        if (enchantment.isSupportedItem(new ItemStack(Items.NETHERITE_SWORD))) {
            cir.setReturnValue(true);
        }
    }
}
