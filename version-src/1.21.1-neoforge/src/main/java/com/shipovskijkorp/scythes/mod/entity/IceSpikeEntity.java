package com.shipovskijkorp.scythes.mod.entity;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.ability.DamageAttributionTracker;
import com.shipovskijkorp.scythes.mod.ability.ScytheAdvancementTracker;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.util.ScytheCombatUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

/** Arrow-like projectile fired by the Frozen Scythe. */
public final class IceSpikeEntity extends PersistentProjectileEntity {

    public IceSpikeEntity(EntityType<? extends IceSpikeEntity> entityType, World world) {
        super(entityType, world);
        this.pickupType = PickupPermission.DISALLOWED;
    }

    public IceSpikeEntity(World world, LivingEntity owner, ItemStack weaponStack) {
        super(
                ScytheMod.ICE_SPIKE,
                owner,
                world,
                Items.ARROW.getDefaultStack(),
                weaponStack.isEmpty() ? null : weaponStack.copy()
        );
        this.pickupType = PickupPermission.DISALLOWED;
    }

    @Override
    protected ItemStack getDefaultItemStack() {
        return Items.ARROW.getDefaultStack();
    }

    @Override
    protected ItemStack asItemStack() {
        return ItemStack.EMPTY;
    }

    @Override
    protected boolean canHit(Entity entity) {
        if (!super.canHit(entity)) return false;

        Entity owner = getOwner();
        if (entity instanceof LivingEntity livingTarget && owner instanceof LivingEntity livingOwner) {
            return !ScytheCombatUtil.isInvalidHostileTarget(livingOwner, livingTarget);
        }

        return true;
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        if (getWorld().isClient) return;

        Entity hitEntity = entityHitResult.getEntity();
        Entity owner = getOwner();

        if (hitEntity instanceof LivingEntity target) {
            target.damage(getDamageSources().arrow(this, owner), ScytheBalance.IceSpike.HIT_DAMAGE);
            target.addStatusEffect(new StatusEffectInstance(
                    ScytheMod.FREEZING,
                    ScytheBalance.IceSpike.FREEZING_TICKS,
                    ScytheBalance.IceSpike.FREEZING_AMPLIFIER,
                    false,
                    true,
                    true
            ));
            if (owner instanceof ServerPlayerEntity playerOwner) {
                DamageAttributionTracker.recordFreezing(target, playerOwner, ScytheBalance.IceSpike.FREEZING_TICKS);
                ScytheAdvancementTracker.markFrozenSpecial(playerOwner);
            }
        }

        playImpactSound();
        discard();
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        if (!getWorld().isClient) {
            playImpactSound();
            discard();
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (getWorld().isClient && !inGround) {
            getWorld().addParticle(ParticleTypes.SNOWFLAKE, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
        }

        if (!getWorld().isClient && age > ScytheBalance.IceSpike.MAX_LIFETIME_TICKS) {
            discard();
        }
    }

    private void playImpactSound() {
        getWorld().playSound(
                null,
                getX(),
                getY(),
                getZ(),
                SoundEvents.BLOCK_GLASS_BREAK,
                SoundCategory.PLAYERS,
                0.7F,
                1.6F
        );
    }
}
