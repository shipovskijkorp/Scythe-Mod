package com.shipovskijkorp.scythes.mod.entity;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.ability.ScytheAdvancementTracker;
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
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

/** Arrow-like projectile fired by the Frost Scythe. */
public final class IceSpikeEntity extends PersistentProjectileEntity {

    public static final float HIT_DAMAGE = 10.0F;
    public static final int FREEZING_TICKS = 30;
    public static final int MAX_LIFETIME_TICKS = 20 * 10;

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
        if (!(getEntityWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

        Entity hitEntity = entityHitResult.getEntity();
        Entity owner = getOwner();

        if (hitEntity instanceof LivingEntity target) {
            Entity attacker = owner != null ? owner : this;
            target.damage(serverWorld, getDamageSources().arrow(this, attacker), HIT_DAMAGE);
            target.addStatusEffect(new StatusEffectInstance(
                    ScytheMod.FREEZING,
                    FREEZING_TICKS,
                    0,
                    false,
                    true,
                    true
            ));
            if (owner instanceof ServerPlayerEntity playerOwner) {
                ScytheAdvancementTracker.markFrozenSpecial(playerOwner);
            }
        }

        playImpactSound(serverWorld);
        discard();
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        if (getEntityWorld() instanceof ServerWorld serverWorld) {
            playImpactSound(serverWorld);
            discard();
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (getEntityWorld().isClient() && !isInGround()) {
            getEntityWorld().addParticleClient(
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
        }

        if (!getEntityWorld().isClient() && age > MAX_LIFETIME_TICKS) {
            discard();
        }
    }

    private void playImpactSound(ServerWorld world) {
        world.playSound(
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
