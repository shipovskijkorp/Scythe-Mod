package com.shipovskijkorp.scythes.mod.mixin;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemGroup.class)
public abstract class ItemGroupMixin {

    /**
     * В 1.20+ ItemGroup#getIcon() кеширует ItemStack (поле icon), поэтому просто Supplier
     * в билдере НЕ будет переключаться. Этот миксин подменяет иконку на лету.
     */
    @Inject(method = "getIcon", at = @At("HEAD"), cancellable = true)
    private void scythes$dynamicTabIcon(CallbackInfoReturnable<ItemStack> cir) {
        Identifier id = Registries.ITEM_GROUP.getId((ItemGroup) (Object) this);
        if (id != null && id.equals(ScytheMod.SCYTHE_ITEM_GROUP_ID)) {
            cir.setReturnValue(ScytheMod.createRotatingTabIcon());
        }
    }
}
