package com.shipovskijkorp.scythes.mod.mixin;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.ability.BloodScytheVampirism;
import com.shipovskijkorp.scythes.mod.ability.GoldenLootMarkTracker;
import com.shipovskijkorp.scythes.mod.ability.GoldenScytheLootingContext;
import com.shipovskijkorp.scythes.mod.ability.ScytheAdvancementTracker;
import com.shipovskijkorp.scythes.mod.ability.WitheringSoulHandler;
import com.shipovskijkorp.scythes.mod.item.BloodScytheItem;
import com.shipovskijkorp.scythes.mod.util.ScytheDamageTypes;
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

    @Unique
    private boolean scythes$bloodDefensePierceQueued;

    @Unique
    private boolean scythes$applyingBloodPierceDamage;

    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    private void scythes$noJump(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.hasStatusEffect(ScytheMod.NO_JUMP) || self.hasStatusEffect(ScytheMod.FREEZING)) {
            ci.cancel();
        }
    }

    @Inject(method = "damage", at = @At("HEAD"))
    private void scythes$captureDamageBefore(DamageSource source,
                                             float amount,
                                             CallbackInfoReturnable<Boolean> cir) {
        if (scythes$applyingBloodPierceDamage) return;

        LivingEntity self = (LivingEntity) (Object) this;
        scythes$healthBeforeDamage = self.getHealth();
        scythes$absorptionBeforeDamage = self.getAbsorptionAmount();
        scythes$bloodDefensePierceQueued = false;

        if (self.getWorld().isClient) return;

        ServerPlayerEntity player = scythes$getBloodScytheAttacker(source);
        if (player == null) return;

        scythes$bloodDefensePierceQueued = BloodScytheItem.DEFENSE_PIERCE_CHANCE > 0.0D
                && player.getRandom().nextDouble() < BloodScytheItem.DEFENSE_PIERCE_CHANCE;
    }

    @Inject(method = "damage", at = @At("RETURN"))
    private void scythes$tryBloodScytheVampirism(DamageSource source,
                                                 float amount,
                                                 CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return;
        if (scythes$applyingBloodPierceDamage) return;

        LivingEntity self = (LivingEntity) (Object) this;
        if (self.getWorld().isClient) return;

        ServerPlayerEntity player = scythes$getBloodScytheAttacker(source);
        if (player == null) return;

        float before = scythes$healthBeforeDamage + scythes$absorptionBeforeDamage;
        float afterBaseHit = self.getHealth() + self.getAbsorptionAmount();
        float baseActualDamage = Math.max(0.0f, before - afterBaseHit);
        float bonusActualDamage = 0.0f;

        if (scythes$bloodDefensePierceQueued && self.isAlive()) {
            float mitigatedDamage = Math.max(0.0f, amount - baseActualDamage);
            float bonusDamage = mitigatedDamage * (float) BloodScytheItem.DEFENSE_PIERCE_MITIGATION_IGNORED;

            if (bonusDamage > 0.0f) {
                float beforeBonus = self.getHealth() + self.getAbsorptionAmount();
                scythes$applyingBloodPierceDamage = true;
                try {
                    self.damage(ScytheDamageTypes.bloodPierce(self.getWorld(), player), bonusDamage);
                } finally {
                    scythes$applyingBloodPierceDamage = false;
                }
                float afterBonus = self.getHealth() + self.getAbsorptionAmount();
                bonusActualDamage = Math.max(0.0f, beforeBonus - afterBonus);
            }
        }

        scythes$bloodDefensePierceQueued = false;
        BloodScytheVampirism.tryHeal(player, baseActualDamage + bonusActualDamage);
    }

    @Unique
    private ServerPlayerEntity scythes$getBloodScytheAttacker(DamageSource source) {
        if (!source.isOf(DamageTypes.PLAYER_ATTACK)) return null;

        Entity attacker = source.getAttacker();
        if (!(attacker instanceof ServerPlayerEntity player)) return null;
        if (!(player.getMainHandStack().getItem() instanceof BloodScytheItem)) return null;

        return player;
    }

    @Inject(method = "onDeath", at = @At("HEAD"))
    private void scythes$awardTrackedWitheringSoul(DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.getWorld().isClient) return;
        WitheringSoulHandler.tryAwardTrackedWitheringDeath(self, source);
        ScytheAdvancementTracker.tryGrantMercilessOnDeath(self, source);
    }


    @Inject(method = "dropLoot", at = @At("HEAD"))
    private void scythes$enterGoldenLootingContext(DamageSource source, boolean causedByPlayer, CallbackInfo ci) {
        GoldenScytheLootingContext.enter((LivingEntity) (Object) this);
    }

    @Inject(method = "dropLoot", at = @At("RETURN"))
    private void scythes$exitGoldenLootingContext(DamageSource source, boolean causedByPlayer, CallbackInfo ci) {
        GoldenScytheLootingContext.exit();
    }

    @Inject(method = "onDeath", at = @At("RETURN"))
    private void scythes$clearGoldenLootMark(DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.getWorld().isClient) return;
        GoldenLootMarkTracker.clear(self);
    }

}
