package com.shipovskijkorp.scythes.mod.mixin;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.ability.BloodScytheVampirism;
import com.shipovskijkorp.scythes.mod.ability.WitheringSoulHandler;
import com.shipovskijkorp.scythes.mod.item.BloodScytheItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Unique
    private float scythes$healthBeforeDamage;

    @Unique
    private float scythes$absorptionBeforeDamage;

    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    private void scythes$noJump(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.hasStatusEffect(ScytheMod.NO_JUMP)) {
            ci.cancel();
        }
    }

    @Inject(method = "damage", at = @At("HEAD"))
    private void scythes$captureDamageBefore(DamageSource source,
                                             float amount,
                                             CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        scythes$healthBeforeDamage = self.getHealth();
        scythes$absorptionBeforeDamage = self.getAbsorptionAmount();
    }

    @Inject(method = "damage", at = @At("RETURN"))
    private void scythes$tryBloodScytheVampirism(DamageSource source,
                                                 float amount,
                                                 CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return;
        if (!source.isOf(DamageTypes.PLAYER_ATTACK)) return;

        LivingEntity self = (LivingEntity) (Object) this;
        if (self.getWorld().isClient) return;

        Entity attacker = source.getAttacker();
        if (!(attacker instanceof ServerPlayerEntity player)) return;
        if (!(player.getMainHandStack().getItem() instanceof BloodScytheItem)) return;

        float before = scythes$healthBeforeDamage + scythes$absorptionBeforeDamage;
        float after = self.getHealth() + self.getAbsorptionAmount();
        float actualDamage = Math.max(0.0f, before - after);

        BloodScytheVampirism.tryHeal(player, actualDamage);
    }

    @Inject(method = "onDeath", at = @At("HEAD"))
    private void scythes$awardTrackedWitheringSoul(DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.getWorld().isClient) return;
        WitheringSoulHandler.tryAwardTrackedWitheringDeath(self, source);
    }

}
