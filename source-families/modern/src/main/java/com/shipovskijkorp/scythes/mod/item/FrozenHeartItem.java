package com.shipovskijkorp.scythes.mod.item;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/** Edible frozen heart with custom and vanilla freezing visuals. */
public final class FrozenHeartItem extends Item {

    private static final int FREEZING_DURATION_TICKS = 3 * 20;

    public FrozenHeartItem(Settings settings) {
        super(settings);
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        ItemStack result = super.finishUsing(stack, world, user);

        if (!world.isClient) {
            user.addStatusEffect(new StatusEffectInstance(
                    ScytheMod.FREEZING,
                    FREEZING_DURATION_TICKS,
                    0,
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
