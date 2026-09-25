package com.shipovskijkorp.scythes.mod.entity;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.ability.ScytheAdvancementTracker;
import com.shipovskijkorp.scythes.mod.util.BurnsUtil;
import com.shipovskijkorp.scythes.mod.util.FireTargeting;
import java.util.List;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.GhastEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Homing Fire Scythe projectile. It never modifies blocks. */
public final class FireballEntity extends ThrownItemEntity {
    private static final String LOCKED_TARGET_KEY = "LockedTarget";
    private LivingEntity lockedTarget;
    private UUID lockedTargetId;
    private int unresolvedTargetTicks;
    private int lostSightTicks;

    public FireballEntity(EntityType<? extends FireballEntity> type, World world) {
        super(type, world);
        setNoGravity(true);
    }

    public FireballEntity(World world, LivingEntity owner, LivingEntity lockedTarget) {
        super(ScytheMod.FIREBALL, owner, world);
        this.lockedTarget = lockedTarget;
        this.lockedTargetId = lockedTarget == null ? null : lockedTarget.getUuid();
        setNoGravity(true);
    }


    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        UUID targetId = lockedTarget != null ? lockedTarget.getUuid() : lockedTargetId;
        if (targetId != null) nbt.putString(LOCKED_TARGET_KEY, targetId.toString());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        String value = nbt.getString(LOCKED_TARGET_KEY);
        try { lockedTargetId = value.isEmpty() ? null : UUID.fromString(value); }
        catch (IllegalArgumentException ignored) { lockedTargetId = null; }
        lockedTarget = null;
        unresolvedTargetTicks = 0;
        lostSightTicks = 0;
    }

    @Override
    protected Item getDefaultItem() {
        return Items.FIRE_CHARGE;
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        if (!getWorld().isClient) explode();
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient) {
            getWorld().addParticle(ParticleTypes.FLAME, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
            getWorld().addParticle(ParticleTypes.SMOKE, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
            return;
        }
        homeTowardsTarget();
        if (age >= ScytheBalance.Fire.FIREBALL_MAX_LIFETIME_TICKS) explode();
    }

    private void homeTowardsTarget() {
        refreshLockedTarget();
        resolveLockedTarget();
        if (lockedTarget == null) return;

        if (!lockedTarget.canSee(this)) {
            if (++lostSightTicks > ScytheBalance.Fire.FIREBALL_LOS_GRACE_TICKS) {
                clearLockedTarget();
                return;
            }
        } else {
            lostSightTicks = 0;
        }

        Vec3d desired = lockedTarget.getPos().add(0.0D, lockedTarget.getHeight() * 0.5D, 0.0D).subtract(getPos());
        if (desired.lengthSquared() < 1.0E-6D) return;
        desired = desired.normalize().multiply(ScytheBalance.Fire.FIREBALL_SPEED);
        Vec3d current = getVelocity();
        Vec3d blended = current.multiply(1.0D - ScytheBalance.Fire.FIREBALL_HOMING_STRENGTH)
                .add(desired.multiply(ScytheBalance.Fire.FIREBALL_HOMING_STRENGTH));
        if (blended.lengthSquared() > 1.0E-6D) setVelocity(blended.normalize().multiply(ScytheBalance.Fire.FIREBALL_SPEED));
    }

    private void refreshLockedTarget() {
        if (lockedTarget == null) return;
        if (lockedTarget.isRemoved()) {
            lockedTarget = null;
            unresolvedTargetTicks = 0;
            return;
        }
        if (!lockedTarget.isAlive() || lockedTarget.getWorld() != getWorld()) clearLockedTarget();
    }

    private void resolveLockedTarget() {
        if (lockedTarget != null || lockedTargetId == null || !(getWorld() instanceof ServerWorld world)) return;
        Entity entity = world.getEntity(lockedTargetId);
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
        if (!(getWorld() instanceof ServerWorld world)) { discard(); return; }

        Entity ownerEntity = getOwner();
        if (ownerEntity instanceof PlayerEntity owner) {
            DamageSource damageSource = getDamageSources().explosion(this, ownerEntity);
            double radius = ScytheBalance.Fire.FIREBALL_EXPLOSION_RADIUS;
            Box box = getBoundingBox().expand(radius);
            List<LivingEntity> targets = world.getEntitiesByClass(
                    LivingEntity.class,
                    box,
                    target -> FireTargeting.isValidTarget(owner, target) && target.squaredDistanceTo(this) <= radius * radius
            );
            for (LivingEntity target : targets) {
                double distance = Math.sqrt(target.squaredDistanceTo(this));
                float damage = (float) (ScytheBalance.Fire.FIREBALL_MAX_DAMAGE * Math.max(0.0D, 1.0D - distance / radius));
                if (damage > 0.0F) {
                    boolean ghastWasAlive = target instanceof GhastEntity && target.isAlive();
                    target.damage(damageSource, damage);
                    if (ghastWasAlive && !target.isAlive() && owner instanceof ServerPlayerEntity playerOwner) {
                        ScytheAdvancementTracker.tryGrantFireballGhastKill(playerOwner);
                    }
                }
            }

            if (lockedTarget != null && lockedTarget.isAlive() && lockedTarget.squaredDistanceTo(this) <= radius * radius
                    && FireTargeting.isValidTarget(owner, lockedTarget)) {
                lockedTarget.setOnFireFor(ScytheBalance.Fire.FIREBALL_FIRE_SECONDS);
                BurnsUtil.tryApplyPassive(lockedTarget);
                lockedTarget.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, ScytheBalance.Fire.FIREBALL_STUN_TICKS, 255, false, false, false));
                lockedTarget.addStatusEffect(new StatusEffectInstance(ScytheMod.NO_JUMP, ScytheBalance.Fire.FIREBALL_STUN_TICKS, 0, false, false, false));
                lockedTarget.setVelocity(0.0D, Math.min(0.0D, lockedTarget.getVelocity().y), 0.0D);
            }
        }

        world.spawnParticles(ParticleTypes.EXPLOSION, getX(), getY(), getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        world.spawnParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), 28, 1.2D, 1.2D, 1.2D, 0.08D);
        world.playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.0F, 1.0F);
        discard();
    }
}
