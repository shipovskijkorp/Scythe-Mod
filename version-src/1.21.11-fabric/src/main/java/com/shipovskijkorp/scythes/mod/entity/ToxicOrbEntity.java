package com.shipovskijkorp.scythes.mod.entity;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.ability.ScytheAdvancementTracker;
import com.shipovskijkorp.scythes.mod.item.ToxicScytheItem;
import com.shipovskijkorp.scythes.mod.util.ScytheCombatUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.List;

public class ToxicOrbEntity extends ThrownItemEntity {

    public static final double DAMAGE_RADIUS = 2.0D;
    public static final float PURE_DAMAGE = 2.0F;
    public static final int POISON_TICKS = 20 * 10;
    public static final int POISON_AMPLIFIER = 1;
    public static final int ARMOR_DAMAGE = 30;
    private static final String ACIDITY_LEVEL_KEY = "AcidityLevel";

    private int acidityLevel;

    public ToxicOrbEntity(EntityType<? extends ToxicOrbEntity> entityType, World world) {
        super(entityType, world);
        setNoGravity(true);
    }

    public ToxicOrbEntity(World world, LivingEntity owner) {
        this(world, owner, 0);
    }

    public ToxicOrbEntity(World world, LivingEntity owner, int acidityLevel) {
        super(ScytheMod.TOXIC_ORB, owner, world, new ItemStack(Items.SLIME_BALL));
        this.acidityLevel = Math.max(0, Math.min(3, acidityLevel));
        setNoGravity(true);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.SLIME_BALL;
    }

    @Override
    protected void writeCustomData(WriteView view) {
        super.writeCustomData(view);
        view.putInt(ACIDITY_LEVEL_KEY, acidityLevel);
    }

    @Override
    protected void readCustomData(ReadView view) {
        super.readCustomData(view);
        acidityLevel = Math.max(0, Math.min(3, view.getInt(ACIDITY_LEVEL_KEY, 0)));
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        if (this.getEntityWorld() instanceof ServerWorld) {
            applyToxicBurst();
            discard();
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (!(this.getEntityWorld() instanceof ServerWorld)) {
            for (int i = 0; i < 2; i++) {
                this.getEntityWorld().addParticleClient(
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

        if (age > 20 * 8) {
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

        Box box = getBoundingBox().expand(DAMAGE_RADIUS);
        List<LivingEntity> targets = this.getEntityWorld().getEntitiesByClass(
                LivingEntity.class,
                box,
                target -> !ScytheCombatUtil.isInvalidHostileTarget(owner, target)
        );

        if (!(this.getEntityWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

        for (LivingEntity target : targets) {
            target.damage(serverWorld, damageSource, PURE_DAMAGE);
            ScytheCombatUtil.refreshStatus(target, StatusEffects.POISON, POISON_TICKS, POISON_AMPLIFIER);
            if (playerOwner != null) {
                ScytheAdvancementTracker.recordToxicPoison(playerOwner, target, POISON_TICKS);
            }
            ScytheCombatUtil.damageArmorSet(target, ToxicScytheItem.applyAcidityArmorDamageBonus(ARMOR_DAMAGE, acidityLevel, target.getRandom()));
        }
    }
}
