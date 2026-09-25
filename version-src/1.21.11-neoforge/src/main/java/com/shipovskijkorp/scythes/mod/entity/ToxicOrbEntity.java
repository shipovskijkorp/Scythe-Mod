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

public class ToxicOrbEntity extends ThrownItemEntity {

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
        this.acidityLevel = Math.max(0, Math.min(ScytheBalance.Enchantments.ACIDITY_MAX_LEVEL, acidityLevel));
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
        List<LivingEntity> targets = this.getEntityWorld().getEntitiesByClass(
                LivingEntity.class,
                box,
                target -> ScytheCombatUtil.isValidCombatTarget(owner, target)
                        && ScytheCombatUtil.isWithinRadius(this, target, ScytheBalance.ToxicOrb.DAMAGE_RADIUS)
        );

        if (!(this.getEntityWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

        for (LivingEntity target : targets) {
            target.damage(serverWorld, damageSource, ScytheBalance.ToxicOrb.PURE_DAMAGE);
            ScytheCombatUtil.refreshStatus(target, StatusEffects.POISON, ScytheBalance.ToxicOrb.POISON_TICKS, ScytheBalance.ToxicOrb.POISON_AMPLIFIER);
            if (playerOwner != null) {
                ScytheAdvancementTracker.recordToxicPoison(playerOwner, target, ScytheBalance.ToxicOrb.POISON_TICKS);
            }
            ScytheCombatUtil.damageArmorSet(target, ToxicScytheItem.applyAcidityArmorDamageBonus(ScytheBalance.ToxicOrb.ARMOR_DAMAGE, acidityLevel, target.getRandom()));
        }
    }
}
