package com.shipovskijkorp.scythes.mod.mixin;

import com.shipovskijkorp.scythes.mod.ScytheMod;
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

    @Inject(method = "isAcceptableItem", at = @At("RETURN"), cancellable = true)
    private void scythes$acceptSwordEnchantmentsOnScythes(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            return;
        }
        if (!ScytheItemUtil.isScythe(stack)) {
            return;
        }

        Enchantment enchantment = (Enchantment) (Object) this;
        if (enchantment == ScytheMod.SPIKED_BLADE) {
            cir.setReturnValue(ScytheItemUtil.isBloodScythe(stack));
            return;
        }

        if (scythes$isSwordAcceptable(enchantment)) {
            cir.setReturnValue(true);
        }
    }

    @Unique
    private static boolean scythes$isSwordAcceptable(Enchantment enchantment) {
        return enchantment.isAcceptableItem(new ItemStack(Items.NETHERITE_SWORD));
    }
}
