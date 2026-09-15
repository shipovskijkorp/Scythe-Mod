package com.shipovskijkorp.scythes.mod.entity;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.ability.ScytheAdvancementTracker;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.util.ScytheCombatUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/** Arrow-like ice projectile fired by the Frozen Scythe. */
public final class IceSpikeEntity extends AbstractArrow {

    public IceSpikeEntity(EntityType<? extends IceSpikeEntity> entityType, Level level) {
        super(entityType, level);
        this.pickup = Pickup.DISALLOWED;
    }

    public IceSpikeEntity(Level level, LivingEntity owner, ItemStack weaponStack) {
        super(
                ScytheMod.ICE_SPIKE,
                owner,
                level,
                Items.ARROW.getDefaultInstance(),
                weaponStack.isEmpty() ? null : weaponStack.copy()
        );
        this.pickup = Pickup.DISALLOWED;
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return Items.ARROW.getDefaultInstance();
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        if (!super.canHitEntity(entity)) return false;

        Entity ownerEntity = getOwner();
        if (entity instanceof LivingEntity target && ownerEntity instanceof LivingEntity owner) {
            return !ScytheCombatUtil.isInvalidHostileTarget(owner, target);
        }
        return true;
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        if (!(level() instanceof ServerLevel serverLevel)) return;

        Entity hit = hitResult.getEntity();
        Entity ownerEntity = getOwner();
        if (hit instanceof LivingEntity target) {
            Entity attacker = ownerEntity != null ? ownerEntity : this;
            target.hurtServer(serverLevel, damageSources().arrow(this, attacker), ScytheBalance.IceSpike.HIT_DAMAGE);
            target.addEffect(new MobEffectInstance(
                    ScytheMod.FREEZING,
                    ScytheBalance.IceSpike.FREEZING_TICKS,
                    ScytheBalance.IceSpike.FREEZING_AMPLIFIER,
                    false,
                    true,
                    true
            ));
            if (ownerEntity instanceof ServerPlayer playerOwner) {
                ScytheAdvancementTracker.markFrozenSpecial(playerOwner);
            }
        }

        playImpactSound(serverLevel);
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult hitResult) {
        if (level() instanceof ServerLevel serverLevel) {
            playImpactSound(serverLevel);
            discard();
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide() && !isInGround()) {
            level().addParticle(
                    ParticleTypes.SNOWFLAKE,
                    false,
                    false,
                    getX(),
                    getY(),
                    getZ(),
                    0.0D,
                    0.0D,
                    0.0D
            );
        } else if (!level().isClientSide() && tickCount > ScytheBalance.IceSpike.MAX_LIFETIME_TICKS) {
            discard();
        }
    }

    private void playImpactSound(ServerLevel level) {
        level.playSound(
                null,
                getX(),
                getY(),
                getZ(),
                SoundEvents.GLASS_BREAK,
                SoundSource.PLAYERS,
                0.7F,
                1.6F
        );
    }
}
