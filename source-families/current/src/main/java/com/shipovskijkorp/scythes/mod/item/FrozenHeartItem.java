package com.shipovskijkorp.scythes.mod.item;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Edible frozen heart that freezes its consumer for three seconds. */
public final class FrozenHeartItem extends Item {

    public FrozenHeartItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        ItemStack result = super.finishUsingItem(stack, level, user);

        if (!level.isClientSide()) {
            user.addEffect(new MobEffectInstance(
                    ScytheMod.FREEZING,
                    ScytheBalance.FrozenHeart.FREEZING_DURATION_TICKS,
                    ScytheBalance.FrozenHeart.FREEZING_AMPLIFIER,
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
