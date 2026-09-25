package com.shipovskijkorp.scythes.mod.entity;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.ability.ScytheAdvancementTracker;
import com.shipovskijkorp.scythes.mod.util.BurnsUtil;
import com.shipovskijkorp.scythes.mod.util.FireTargeting;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Homing Fire Scythe projectile. It never modifies blocks. */
public final class FireballEntity extends ThrowableProjectile implements ItemSupplier {
    private static final String LOCKED_TARGET_KEY = "LockedTarget";
    private LivingEntity lockedTarget;
    private UUID lockedTargetId;
    private int unresolvedTargetTicks;
    private int lostSightTicks;

    public FireballEntity(EntityType<? extends FireballEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public FireballEntity(Level level, LivingEntity owner, LivingEntity lockedTarget) {
        super(ScytheMod.FIREBALL, owner.getX(), owner.getEyeY() - ScytheBalance.Fire.FIREBALL_SPAWN_EYE_OFFSET, owner.getZ(), level);
        setOwner(owner);
        this.lockedTarget = lockedTarget;
        this.lockedTargetId = lockedTarget == null ? null : lockedTarget.getUUID();
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        UUID targetId = lockedTarget != null ? lockedTarget.getUUID() : lockedTargetId;
        if (targetId != null) output.putString(LOCKED_TARGET_KEY, targetId.toString());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        String value = input.getStringOr(LOCKED_TARGET_KEY, "");
        try { lockedTargetId = value.isEmpty() ? null : UUID.fromString(value); }
        catch (IllegalArgumentException ignored) { lockedTargetId = null; }
        lockedTarget = null;
        unresolvedTargetTicks = 0;
        lostSightTicks = 0;
    }


    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.FIRE_CHARGE);
    }

    @Override
    protected void onHit(HitResult hitResult) {
        if (level() instanceof ServerLevel) explode();
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel)) {
            level().addParticle(ParticleTypes.FLAME, false, false, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
            level().addParticle(ParticleTypes.SMOKE, false, false, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
            return;
        }
        homeTowardsTarget();
        if (tickCount >= ScytheBalance.Fire.FIREBALL_MAX_LIFETIME_TICKS) explode();
    }

    private void homeTowardsTarget() {
        refreshLockedTarget();
        resolveLockedTarget();
        if (lockedTarget == null) return;

        if (!lockedTarget.hasLineOfSight(this)) {
            if (++lostSightTicks > ScytheBalance.Fire.FIREBALL_LOS_GRACE_TICKS) {
                clearLockedTarget();
                return;
            }
        } else {
            lostSightTicks = 0;
        }

        Vec3 desired = lockedTarget.position().add(0.0D, lockedTarget.getBbHeight() * 0.5D, 0.0D).subtract(position());
        if (desired.lengthSqr() < 1.0E-6D) return;
        desired = desired.normalize().scale(ScytheBalance.Fire.FIREBALL_SPEED);
        Vec3 current = getDeltaMovement();
        Vec3 blended = current.scale(1.0D - ScytheBalance.Fire.FIREBALL_HOMING_STRENGTH)
                .add(desired.scale(ScytheBalance.Fire.FIREBALL_HOMING_STRENGTH));
        if (blended.lengthSqr() > 1.0E-6D) setDeltaMovement(blended.normalize().scale(ScytheBalance.Fire.FIREBALL_SPEED));
    }

    private void refreshLockedTarget() {
        if (lockedTarget == null) return;
        if (lockedTarget.isRemoved()) {
            lockedTarget = null;
            unresolvedTargetTicks = 0;
            return;
        }
        if (!lockedTarget.isAlive() || lockedTarget.level() != level()) clearLockedTarget();
    }

    private void resolveLockedTarget() {
        if (lockedTarget != null || lockedTargetId == null || !(level() instanceof ServerLevel serverLevel)) return;
        Entity entity = serverLevel.getEntity(lockedTargetId);
        if (entity instanceof LivingEntity living && living.isAlive()) {
            lockedTarget = living;
            unresolvedTargetTicks = 0;
            return;
        }
        if (++unresolvedTargetTicks > ScytheBalance.Fire.FIREBALL_TARGET_REACQUIRE_TICKS) clearLockedTarget();
    }

    private void clearLockedTarget() {
        lockedTarget = null;
        lockedTargetId = null;
        unresolvedTargetTicks = 0;
        lostSightTicks = 0;
    }

    private void explode() {
        resolveLockedTarget();
        if (isRemoved()) return;
        if (!(level() instanceof ServerLevel serverLevel)) { discard(); return; }
        Entity ownerEntity = getOwner();
        if (ownerEntity instanceof Player owner) {
            DamageSource source = damageSources().explosion(this, ownerEntity);
            double radius = ScytheBalance.Fire.FIREBALL_EXPLOSION_RADIUS;
            AABB box = getBoundingBox().inflate(radius);
            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(
                    LivingEntity.class,
                    box,
                    target -> FireTargeting.isValidTarget(owner, target) && target.distanceToSqr(this) <= radius * radius
            );
            for (LivingEntity target : targets) {
                double distance = Math.sqrt(target.distanceToSqr(this));
                float damage = (float) (ScytheBalance.Fire.FIREBALL_MAX_DAMAGE * Math.max(0.0D, 1.0D - distance / radius));
                if (damage > 0.0F) {
                    boolean ghastWasAlive = target instanceof Ghast && target.isAlive();
                    target.hurtServer(serverLevel, source, damage);
                    if (ghastWasAlive && !target.isAlive() && owner instanceof ServerPlayer playerOwner) {
                        ScytheAdvancementTracker.tryGrantFireballGhastKill(playerOwner);
                    }
                }
            }
            if (lockedTarget != null && lockedTarget.isAlive() && lockedTarget.distanceToSqr(this) <= radius * radius
                    && FireTargeting.isValidTarget(owner, lockedTarget)) {
                lockedTarget.igniteForSeconds(ScytheBalance.Fire.FIREBALL_FIRE_SECONDS);
                BurnsUtil.tryApplyPassive(lockedTarget);
                lockedTarget.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ScytheBalance.Fire.FIREBALL_STUN_TICKS, 255, false, false, false));
                lockedTarget.addEffect(new MobEffectInstance(ScytheMod.NO_JUMP, ScytheBalance.Fire.FIREBALL_STUN_TICKS, 0, false, false, false));
                lockedTarget.setDeltaMovement(0.0D, Math.min(0.0D, lockedTarget.getDeltaMovement().y), 0.0D);
            }
        }
        serverLevel.sendParticles(ParticleTypes.EXPLOSION, getX(), getY(), getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        serverLevel.sendParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), 28, 1.2D, 1.2D, 1.2D, 0.08D);
        serverLevel.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0F, 1.0F);
        discard();
    }
}
