package com.shipovskijkorp.scythes.mod.entity;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.ability.ScytheAdvancementTracker;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.item.ToxicScytheItem;
import com.shipovskijkorp.scythes.mod.util.ScytheCombatUtil;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;

public class ToxicOrbEntity extends ThrowableProjectile implements ItemSupplier {

    private int acidityLevel = 0;

    public ToxicOrbEntity(EntityType<? extends ToxicOrbEntity> entityType, Level world) {
        super(entityType, world);
        setNoGravity(true);
    }

    public ToxicOrbEntity(Level world, LivingEntity owner) {
        super(ScytheMod.TOXIC_ORB, owner.getX(), owner.getEyeY() - ScytheBalance.ToxicOrb.SPAWN_EYE_OFFSET, owner.getZ(), world);
        setOwner(owner);
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // Toxic orb has no custom synced entity data.
    }

    public void setAcidityLevel(int acidityLevel) {
        this.acidityLevel = Math.max(0, Math.min(ScytheBalance.Enchantments.ACIDITY_MAX_LEVEL, acidityLevel));
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.SLIME_BALL);
    }

    @Override
    protected void onHit(HitResult hitResult) {
        if (this.level() instanceof ServerLevel) {
            applyToxicBurst();
            discard();
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (!(this.level() instanceof ServerLevel)) {
            for (int i = 0; i < 2; i++) {
                this.level().addParticle(
                        ParticleTypes.SPORE_BLOSSOM_AIR,
                        false,
                        false,
                        getX(),
                        getY(),
                        getZ(),
                        0.0D,
                        0.0D,
                        0.0D
                );
            }
            return;
        }

        if (tickCount > ScytheBalance.ToxicOrb.MAX_LIFETIME_TICKS) {
            applyToxicBurst();
            discard();
        }
    }

    private void applyToxicBurst() {
        if (!(this.level() instanceof ServerLevel serverWorld)) {
            return;
        }

        Entity ownerEntity = getOwner();
        LivingEntity owner = ownerEntity instanceof LivingEntity livingOwner ? livingOwner : null;
        ServerPlayer playerOwner = ownerEntity instanceof ServerPlayer serverPlayer ? serverPlayer : null;
        DamageSource damageSource = ownerEntity != null
                ? damageSources().indirectMagic(this, ownerEntity)
                : damageSources().magic();

        AABB box = getBoundingBox().inflate(ScytheBalance.ToxicOrb.DAMAGE_RADIUS);
        List<LivingEntity> targets = serverWorld.getEntitiesOfClass(
                LivingEntity.class,
                box,
                target -> !ScytheCombatUtil.isInvalidHostileTarget(owner, target)
        );

        for (LivingEntity target : targets) {
            target.hurtServer(serverWorld, damageSource, ScytheBalance.ToxicOrb.PURE_DAMAGE);
            ScytheCombatUtil.refreshStatus(target, MobEffects.POISON, ScytheBalance.ToxicOrb.POISON_TICKS, ScytheBalance.ToxicOrb.POISON_AMPLIFIER);
            if (playerOwner != null) {
                ScytheAdvancementTracker.recordToxicPoison(playerOwner, target, ScytheBalance.ToxicOrb.POISON_TICKS);
            }
            ScytheCombatUtil.damageArmorSet(target, ToxicScytheItem.applyAcidityBonus(this.random, ScytheBalance.ToxicOrb.ARMOR_DAMAGE, acidityLevel));
        }
    }
}
