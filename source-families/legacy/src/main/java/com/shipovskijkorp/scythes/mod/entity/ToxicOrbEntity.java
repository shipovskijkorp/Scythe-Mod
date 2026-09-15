package com.shipovskijkorp.scythes.mod.entity;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.ability.ScytheAdvancementTracker;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import com.shipovskijkorp.scythes.mod.item.ToxicScytheItem;
import com.shipovskijkorp.scythes.mod.util.ScytheCombatUtil;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

public class ToxicOrbEntity extends ThrownItemEntity {

    private static final String ACIDITY_LEVEL_KEY = "AcidityLevel";

    private int acidityLevel;

    public ToxicOrbEntity(EntityType<? extends ToxicOrbEntity> entityType, World world) {
        super(entityType, world);
        setNoGravity(true);
    }

    public ToxicOrbEntity(World world, LivingEntity owner, int acidityLevel) {
        super(ScytheMod.TOXIC_ORB, owner, world);
        this.acidityLevel = Math.max(0, Math.min(ScytheBalance.Enchantments.ACIDITY_MAX_LEVEL, acidityLevel));
        setNoGravity(true);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.SLIME_BALL;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt(ACIDITY_LEVEL_KEY, acidityLevel);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        acidityLevel = Math.max(0, Math.min(3, nbt.getInt(ACIDITY_LEVEL_KEY)));
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        if (!getWorld().isClient) {
            applyToxicBurst();
            discard();
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (getWorld().isClient) {
            for (int i = 0; i < 2; i++) {
                getWorld().addParticle(
                        ParticleTypes.SPORE_BLOSSOM_AIR,
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

        if (age > ScytheBalance.ToxicOrb.MAX_LIFETIME_TICKS) {
            applyToxicBurst();
            discard();
        }
    }

    private void applyToxicBurst() {
        Entity ownerEntity = getOwner();
        LivingEntity owner = ownerEntity instanceof LivingEntity livingOwner ? livingOwner : null;
        ServerPlayerEntity playerOwner = ownerEntity instanceof ServerPlayerEntity serverPlayer ? serverPlayer : null;
        DamageSource damageSource = ownerEntity != null
                ? getDamageSources().indirectMagic(this, ownerEntity)
                : getDamageSources().magic();

        Box box = getBoundingBox().expand(ScytheBalance.ToxicOrb.DAMAGE_RADIUS);
        List<LivingEntity> targets = getWorld().getEntitiesByClass(
                LivingEntity.class,
                box,
                target -> !ScytheCombatUtil.isInvalidHostileTarget(owner, target)
        );

        for (LivingEntity target : targets) {
            target.damage(damageSource, ScytheBalance.ToxicOrb.PURE_DAMAGE);
            ScytheCombatUtil.refreshStatus(target, StatusEffects.POISON, ScytheBalance.ToxicOrb.POISON_TICKS, ScytheBalance.ToxicOrb.POISON_AMPLIFIER);
            if (playerOwner != null) {
                ScytheAdvancementTracker.recordToxicPoison(playerOwner, target, ScytheBalance.ToxicOrb.POISON_TICKS);
            }
            int armorDamage = ToxicScytheItem.applyAcidityArmorDamageBonus(ScytheBalance.ToxicOrb.ARMOR_DAMAGE, acidityLevel, target.getRandom());
            ScytheCombatUtil.damageArmorSet(target, armorDamage);
        }
    }
}
