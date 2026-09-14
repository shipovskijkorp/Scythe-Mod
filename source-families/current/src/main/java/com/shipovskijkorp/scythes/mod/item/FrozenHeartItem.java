package com.shipovskijkorp.scythes.mod.item;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Edible frozen heart that freezes its consumer for three seconds. */
public final class FrozenHeartItem extends Item {

    private static final int FREEZING_DURATION_TICKS = 3 * 20;

    public FrozenHeartItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        ItemStack result = super.finishUsingItem(stack, level, user);

        if (!level.isClientSide()) {
            user.addEffect(new MobEffectInstance(
                    ScytheMod.FREEZING,
                    FREEZING_DURATION_TICKS,
                    0,
                    false,
                    true,
                    true
            ));
            user.setTicksFrozen(Math.max(
                    user.getTicksFrozen(),
                    Math.max(0, user.getTicksRequiredToFreeze() - 1)
            ));
        }

        return result;
    }
}
