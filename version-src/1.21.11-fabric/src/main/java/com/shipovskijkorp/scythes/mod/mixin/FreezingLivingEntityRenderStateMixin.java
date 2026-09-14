package com.shipovskijkorp.scythes.mod.mixin;

import com.shipovskijkorp.scythes.mod.util.FreezingRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntityRenderState.class)
public abstract class FreezingLivingEntityRenderStateMixin implements FreezingRenderState {

    @Unique
    private boolean scythes$frozenForRendering;

    @Override
    public boolean scythes$isFrozenForRendering() {
        return scythes$frozenForRendering;
    }

    @Override
    public void scythes$setFrozenForRendering(boolean frozen) {
        scythes$frozenForRendering = frozen;
    }
}
