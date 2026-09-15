package com.shipovskijkorp.scythes.mod.mixin;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CreativeModeTab.class)
public abstract class ItemGroupMixin {

    /**
     * В 1.20+ CreativeModeTab#getIconItem() кеширует ItemStack (поле icon), поэтому просто Supplier
     * в билдере НЕ будет переключаться. Этот миксин подменяет иконку на лету.
     */
    @Inject(method = "getIconItem", at = @At("HEAD"), cancellable = true)
    private void scythes$dynamicTabIcon(CallbackInfoReturnable<ItemStack> cir) {
        Identifier id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey((CreativeModeTab) (Object) this);
        if (id != null && id.equals(ScytheMod.SCYTHE_ITEM_GROUP_ID)) {
            cir.setReturnValue(ScytheMod.createRotatingTabIcon());
        }
    }
}
