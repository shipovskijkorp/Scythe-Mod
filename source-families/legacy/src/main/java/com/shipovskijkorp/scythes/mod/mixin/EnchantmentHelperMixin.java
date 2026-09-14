package com.shipovskijkorp.scythes.mod.mixin;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.ability.GoldenScytheLootingContext;
import com.shipovskijkorp.scythes.mod.item.ScytheItemUtil;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {

    @Inject(method = "getPossibleEntries", at = @At("RETURN"), cancellable = true)
    private static void scythes$makeScythesEnchantLikeSwords(int power, ItemStack stack, boolean treasureAllowed, CallbackInfoReturnable<List<EnchantmentLevelEntry>> cir) {
        List<EnchantmentLevelEntry> entries = new ArrayList<>(cir.getReturnValue());

        scythes$removeWrongScytheEnchantments(stack, entries);

        if (ScytheItemUtil.isScythe(stack)) {
            scythes$addMissingSwordEntries(power, treasureAllowed, entries);
        }

        cir.setReturnValue(entries);
    }


    private static void scythes$removeWrongScytheEnchantments(ItemStack stack, List<EnchantmentLevelEntry> entries) {
        if (!ScytheItemUtil.isBloodScythe(stack)) {
            entries.removeIf(entry -> entry.enchantment == ScytheMod.SPIKED_BLADE);
        }
        if (!ScytheItemUtil.isWitheringScythe(stack)) {
            entries.removeIf(entry -> entry.enchantment == ScytheMod.ADDITIONAL_SLOT || entry.enchantment == ScytheMod.SOUL_SIPHON);
        }
        if (!ScytheItemUtil.isToxicScythe(stack)) {
            entries.removeIf(entry -> entry.enchantment == ScytheMod.ACIDITY);
        }
    }

    private static void scythes$addMissingSwordEntries(int power, boolean treasureAllowed, List<EnchantmentLevelEntry> entries) {
        ItemStack referenceSword = new ItemStack(Items.NETHERITE_SWORD);
        Set<Enchantment> presentEnchantments = new HashSet<>();
        for (EnchantmentLevelEntry entry : entries) {
            presentEnchantments.add(entry.enchantment);
        }

        for (Enchantment enchantment : Registries.ENCHANTMENT) {
            if (presentEnchantments.contains(enchantment)) {
                continue;
            }
            if (enchantment == ScytheMod.SPIKED_BLADE) {
                continue;
            }
            if (enchantment.isTreasure() && !treasureAllowed) {
                continue;
            }
            if (!enchantment.isAvailableForRandomSelection()) {
                continue;
            }
            if (!enchantment.isAcceptableItem(referenceSword)) {
                continue;
            }

            for (int level = enchantment.getMaxLevel(); level >= enchantment.getMinLevel(); --level) {
                if (power >= enchantment.getMinPower(level) && power <= enchantment.getMaxPower(level)) {
                    entries.add(new EnchantmentLevelEntry(enchantment, level));
                    presentEnchantments.add(enchantment);
                    break;
                }
            }
        }
    }

    @Inject(method = "getLooting", at = @At("RETURN"), cancellable = true)
    private static void scythes$addGoldenScytheLooting(LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        int bonus = GoldenScytheLootingContext.getLootingBonus(entity);
        if (bonus > 0) {
            cir.setReturnValue(cir.getReturnValue() + bonus);
        }
    }

}
