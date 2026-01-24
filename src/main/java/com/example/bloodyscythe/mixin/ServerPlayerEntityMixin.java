package com.example.bloodyscythe.mixin;

import com.example.bloodyscythe.ability.BloodHarvestTracker;
import com.example.bloodyscythe.item.BloodScytheItem;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {

    @Inject(method = "onDeath", at = @At("HEAD"))
    private void bloodyscythe$onDeath(DamageSource source, CallbackInfo ci) {
        if (!(source.getAttacker() instanceof ServerPlayerEntity killer)) return;

        // безопасные проверки
        if (!BloodHarvestTracker.isActive(killer)) return;
        if (!(killer.getMainHandStack().getItem() instanceof BloodScytheItem)) return;

        BloodHarvestTracker.onKill(killer);
    }
}
