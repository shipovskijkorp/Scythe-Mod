package com.shipovskijkorp.scythes.mod.item;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/** Edible frozen heart with custom and vanilla freezing effects. */
public final class FrozenHeartItem extends Item {

    public FrozenHeartItem(Settings settings) {
        super(settings);
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        ItemStack result = super.finishUsing(stack, world, user);

        if (!world.isClient) {
            user.addStatusEffect(new StatusEffectInstance(
                    ScytheMod.FREEZING,
                    ScytheBalance.FrozenHeart.FREEZING_DURATION_TICKS,
                    ScytheBalance.FrozenHeart.FREEZING_AMPLIFIER,
                    false,
                    true,
                    true
            ));

            user.setFrozenTicks(Math.max(
                    user.getFrozenTicks(),
                    Math.max(0, user.getMinFreezeDamageTicks() - 1)
            ));
        }

        return result;
    }
}
