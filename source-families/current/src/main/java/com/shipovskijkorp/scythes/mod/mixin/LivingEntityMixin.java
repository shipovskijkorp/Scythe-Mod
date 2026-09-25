package com.shipovskijkorp.scythes.mod.mixin;

import com.shipovskijkorp.scythes.mod.util.ScytheCombatUtil;
import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.ability.BloodScytheAttackContext;
import com.shipovskijkorp.scythes.mod.ability.DamageAttributionTracker;
import com.shipovskijkorp.scythes.mod.ability.BloodScytheVampirism;
import com.shipovskijkorp.scythes.mod.ability.GoldenLootMarkTracker;
import com.shipovskijkorp.scythes.mod.ability.GoldenScytheLootingContext;
import com.shipovskijkorp.scythes.mod.ability.ScytheAdvancementTracker;
import com.shipovskijkorp.scythes.mod.ability.WitheringSoulHandler;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.item.BloodScytheItem;
import com.shipovskijkorp.scythes.mod.item.FireScytheItem;
import com.shipovskijkorp.scythes.mod.util.ScytheDamageTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
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

    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    private void scythes$noJump(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.hasEffect(ScytheMod.NO_JUMP)) {
            ci.cancel();
        }
    }

    @Inject(method = "heal", at = @At("HEAD"), cancellable = true)
    private void scythes$blockHealingWhileBurned(float amount, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.hasEffect(ScytheMod.BURNS)) ci.cancel();
    }

    @Inject(method = "canStandOnFluid", at = @At("HEAD"), cancellable = true)
    private void scythes$walkOnLavaWithFireScythe(FluidState fluidState, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof Player player && FireScytheItem.isHeld(player) && fluidState.is(FluidTags.LAVA)) {
            cir.setReturnValue(true);
        }
    }

    @ModifyVariable(method = "hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("HEAD"), argsOnly = true)
    private DamageSource scythes$attributeWitheringDamage(DamageSource source) {
        if (!source.is(DamageTypes.WITHER)) return source;
        LivingEntity self = (LivingEntity) (Object) this;
        ServerPlayer owner = DamageAttributionTracker.getWitheringOwner(self);
        return owner == null ? source : ScytheDamageTypes.withering(self.level(), owner);
    }

    @Inject(method = "hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("HEAD"), cancellable = true)
    private void scythes$ignoreHotFloorWithFireScythe(ServerLevel world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof Player player && FireScytheItem.isHeld(player) && source.is(DamageTypes.HOT_FLOOR)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("HEAD"))
    private void scythes$captureDamageBefore(ServerLevel world,
                                             DamageSource source,
                                             float amount,
                                             CallbackInfoReturnable<Boolean> cir) {
        if (scythes$applyingBloodPierceDamage) return;

        LivingEntity self = (LivingEntity) (Object) this;
        scythes$healthBeforeDamage = self.getHealth();
        scythes$absorptionBeforeDamage = self.getAbsorptionAmount();
        scythes$bloodDefensePierceQueued = false;

        ServerPlayer player = scythes$getBloodScytheAttacker(source);
        if (player == null) return;
        if (ScytheCombatUtil.isProtectedWitheringMinion(player, self)) return;

        scythes$bloodDefensePierceQueued = ScytheBalance.Blood.DEFENSE_PIERCE_CHANCE > 0.0D
                && player.getRandom().nextDouble() < ScytheBalance.Blood.DEFENSE_PIERCE_CHANCE;
    }

    @Inject(method = "hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("RETURN"))
    private void scythes$tryBloodScytheVampirism(ServerLevel world,
                                                 DamageSource source,
                                                 float amount,
                                                 CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return;
        if (scythes$applyingBloodPierceDamage) return;

        LivingEntity self = (LivingEntity) (Object) this;
        ServerPlayer player = scythes$getBloodScytheAttacker(source);
        if (player == null) return;
        if (ScytheCombatUtil.isProtectedWitheringMinion(player, self)) return;

        float before = scythes$healthBeforeDamage + scythes$absorptionBeforeDamage;
        float afterBaseHit = self.getHealth() + self.getAbsorptionAmount();
        float baseActualDamage = Math.max(0.0f, before - afterBaseHit);
        float bonusActualDamage = 0.0f;

        if (scythes$bloodDefensePierceQueued && self.isAlive()) {
            float mitigatedDamage = Math.max(0.0f, amount - baseActualDamage);
            float bonusDamage = mitigatedDamage * (float) ScytheBalance.Blood.DEFENSE_PIERCE_MITIGATION_IGNORED;

            if (bonusDamage > 0.0f) {
                float beforeBonus = self.getHealth() + self.getAbsorptionAmount();
                scythes$applyingBloodPierceDamage = true;
                try {
                    self.hurtServer(world, ScytheDamageTypes.bloodPierce(world, player), bonusDamage);
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
    private ServerPlayer scythes$getBloodScytheAttacker(DamageSource source) {
        if (!source.is(DamageTypes.PLAYER_ATTACK)) return null;

        Entity attacker = source.getEntity();
        if (!(attacker instanceof ServerPlayer player)) return null;
        if (!(player.getMainHandItem().getItem() instanceof BloodScytheItem)
                && !BloodScytheAttackContext.isActive(player.getUUID())) return null;

        return player;
    }

    @Inject(method = "die(Lnet/minecraft/world/damagesource/DamageSource;)V", at = @At("HEAD"))
    private void scythes$awardTrackedWitheringSoul(DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        WitheringSoulHandler.tryAwardTrackedWitheringDeath(self, source);
        ScytheAdvancementTracker.tryGrantMercilessOnDeath(self, source);
        ScytheAdvancementTracker.tryGrantFarmerDragonKill(self, source);
    }

    @Inject(method = "dropAllDeathLoot(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;)V", at = @At("HEAD"))
    private void scythes$enterGoldenLootingContext(ServerLevel world, DamageSource source, CallbackInfo ci) {
        GoldenScytheLootingContext.enter((LivingEntity) (Object) this);
    }

    @Inject(method = "dropAllDeathLoot(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;)V", at = @At("RETURN"))
    private void scythes$exitGoldenLootingContext(ServerLevel world, DamageSource source, CallbackInfo ci) {
        GoldenScytheLootingContext.exit();
    }

    @Inject(method = "die(Lnet/minecraft/world/damagesource/DamageSource;)V", at = @At("RETURN"))
    private void scythes$clearGoldenLootMark(DamageSource source, CallbackInfo ci) {
        GoldenLootMarkTracker.clear((LivingEntity) (Object) this);
    }
}
