package com.shipovskijkorp.scythes.mod.mixin;

import com.shipovskijkorp.scythes.mod.ability.GoldenScytheLootingContext;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {

    @Inject(method = "getEquipmentLevel", at = @At("RETURN"), cancellable = true)
    private static void scythes$addGoldenScytheLooting(RegistryEntry<Enchantment> enchantment,
                                                       LivingEntity entity,
                                                       CallbackInfoReturnable<Integer> cir) {
        if (!enchantment.matchesKey(Enchantments.LOOTING)) return;

        int bonus = GoldenScytheLootingContext.getLootingBonus(entity);
        if (bonus > 0) {
            cir.setReturnValue(cir.getReturnValue() + bonus);
        }
    }
}
