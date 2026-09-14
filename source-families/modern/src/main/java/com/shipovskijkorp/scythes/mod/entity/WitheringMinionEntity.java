package com.shipovskijkorp.scythes.mod.entity;

import com.shipovskijkorp.scythes.mod.ability.WitheringMinionManager;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;

public class WitheringMinionEntity extends WitherSkeletonEntity {

    private static final String OWNER_KEY = "Owner";
    private static final String LIFE_TICKS_KEY = "LifeTicks";

    public static final float MAX_HEALTH = 20.0F;
    public static final double ARMOR = 14.0D;
    public static final int LIFETIME_TICKS = 20 * 60 * 15;
    public static final int REGEN_IDLE_TICKS = 20 * 5;
    public static final int REGEN_INTERVAL_TICKS = 20;
    public static final float REGEN_HEALTH_PER_TICK = 1.0F;
    public static final int REGEN_DURABILITY_COST = 1;
    private static final double OWNER_TELEPORT_DISTANCE_SQUARED = 12.0D * 12.0D;

    @Nullable
    private UUID ownerUuid;
    private int lifeTicks;
    private long lastDamageWorldTick;

    public WitheringMinionEntity(EntityType<? extends WitheringMinionEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 0;
        this.setCanPickUpLoot(false);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, MAX_HEALTH)
                .add(EntityAttributes.GENERIC_ARMOR, ARMOR)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.add(3, new FollowOwnerLikeWolfGoal(this, 1.15D, 5.0F, 2.5F));
        this.goalSelector.add(7, new WanderAroundFarGoal(this, 0.8D));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(8, new LookAroundGoal(this));
    }

    public void initializeForOwner(ServerPlayerEntity owner) {
        this.ownerUuid = owner.getUuid();
        this.lifeTicks = 0;
        this.lastDamageWorldTick = getWorld().getTime();
        this.setCustomNameVisible(false);
        this.setPersistent();
        this.setHealth(MAX_HEALTH);
        setAttributeBase(EntityAttributes.GENERIC_MAX_HEALTH, MAX_HEALTH);
        setAttributeBase(EntityAttributes.GENERIC_ARMOR, ARMOR);
        this.setHealth(MAX_HEALTH);
        this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD));
        this.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    private void setAttributeBase(RegistryEntry<EntityAttribute> attribute, double value) {
        EntityAttributeInstance instance = this.getAttributeInstance(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (getWorld().isClient) {
            return;
        }

        lifeTicks++;
        if (lifeTicks >= LIFETIME_TICKS || getOwnerPlayer() == null) {
            discard();
            return;
        }

        if (age % 10 == 0) {
            updateDogLikeTarget();
        }

        tryPassiveRegeneration();
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean damaged = super.damage(source, amount);
        if (!getWorld().isClient && damaged && amount > 0.0F) {
            lastDamageWorldTick = getWorld().getTime();
        }
        return damaged;
    }

    private void tryPassiveRegeneration() {
        if (age % REGEN_INTERVAL_TICKS != 0) return;
        if (getHealth() >= getMaxHealth()) return;

        long now = getWorld().getTime();
        if (now - lastDamageWorldTick < REGEN_IDLE_TICKS) return;

        ServerPlayerEntity owner = getOwnerPlayer();
        if (owner == null) return;

        if (WitheringMinionManager.damageOwnerScytheForMinionRegen(owner, REGEN_DURABILITY_COST)) {
            heal(REGEN_HEALTH_PER_TICK);
        }
    }

    private void updateDogLikeTarget() {
        ServerPlayerEntity owner = getOwnerPlayer();
        if (owner == null) return;

        LivingEntity current = getTarget();
        if (isAllowedTarget(owner, current)) {
            return;
        }

        LivingEntity selfAttacker = getAttacker();
        if (isAllowedTarget(owner, selfAttacker)) {
            setTarget(selfAttacker);
            return;
        }

        LivingEntity ownerAttacker = owner.getAttacker();
        if (isAllowedTarget(owner, ownerAttacker)) {
            setTarget(ownerAttacker);
            return;
        }

        LivingEntity ownerTarget = owner.getAttacking();
        if (isAllowedTarget(owner, ownerTarget)) {
            setTarget(ownerTarget);
            return;
        }

        setTarget(null);
    }

    private boolean isAllowedTarget(ServerPlayerEntity owner, @Nullable LivingEntity target) {
        if (target == null) return false;
        if (target instanceof WitheringMinionEntity minion && minion.isOwner(owner)) return false;
        return !com.shipovskijkorp.scythes.mod.util.ScytheCombatUtil.isInvalidHostileTarget(owner, target);
    }

    @Nullable
    public ServerPlayerEntity getOwnerPlayer() {
        if (ownerUuid == null || getWorld().getServer() == null) {
            return null;
        }
        return getWorld().getServer().getPlayerManager().getPlayer(ownerUuid);
    }

    public boolean isOwner(ServerPlayerEntity player) {
        return ownerUuid != null && ownerUuid.equals(player.getUuid());
    }

    @Override
    protected boolean isAffectedByDaylight() {
        return false;
    }

    private boolean shouldTeleportToOwner(ServerPlayerEntity owner) {
        return this.squaredDistanceTo(owner) >= OWNER_TELEPORT_DISTANCE_SQUARED;
    }

    private boolean tryTeleportNearOwner(ServerPlayerEntity owner) {
        int ownerX = MathHelper.floor(owner.getX());
        int ownerY = MathHelper.floor(owner.getY());
        int ownerZ = MathHelper.floor(owner.getZ());

        for (int i = 0; i < 10; i++) {
            int dx = this.getRandom().nextInt(7) - 3;
            int dy = this.getRandom().nextInt(3) - 1;
            int dz = this.getRandom().nextInt(7) - 3;

            if (Math.abs(dx) < 2 && Math.abs(dz) < 2) {
                continue;
            }

            if (tryTeleportTo(ownerX + dx, ownerY + dy, ownerZ + dz)) {
                return true;
            }
        }

        return false;
    }

    private boolean tryTeleportTo(int x, int y, int z) {
        BlockPos targetPos = new BlockPos(x, y, z);
        BlockPos groundPos = targetPos.down();

        if (!getWorld().getBlockState(groundPos).isSideSolidFullSquare(getWorld(), groundPos, Direction.UP)) {
            return false;
        }
        if (!getWorld().getBlockState(targetPos).isAir() || !getWorld().getBlockState(targetPos.up()).isAir()) {
            return false;
        }
        if (!getWorld().getFluidState(targetPos).isEmpty() || !getWorld().getFluidState(targetPos.up()).isEmpty()) {
            return false;
        }

        double targetX = x + 0.5D;
        double targetY = y;
        double targetZ = z + 0.5D;
        Box targetBox = this.getBoundingBox().offset(targetX - getX(), targetY - getY(), targetZ - getZ());
        if (!getWorld().isSpaceEmpty(this, targetBox)) {
            return false;
        }

        this.refreshPositionAndAngles(targetX, targetY, targetZ, getYaw(), getPitch());
        this.getNavigation().stop();
        return true;
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (ownerUuid != null) {
            nbt.putUuid(OWNER_KEY, ownerUuid);
        }
        nbt.putInt(LIFE_TICKS_KEY, lifeTicks);
        nbt.putLong("LastDamageWorldTick", lastDamageWorldTick);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.containsUuid(OWNER_KEY)) {
            ownerUuid = nbt.getUuid(OWNER_KEY);
        }
        lifeTicks = Math.max(0, nbt.getInt(LIFE_TICKS_KEY));
        lastDamageWorldTick = nbt.contains("LastDamageWorldTick") ? nbt.getLong("LastDamageWorldTick") : getWorld().getTime();
    }

    private static final class FollowOwnerLikeWolfGoal extends Goal {
        private final WitheringMinionEntity minion;
        private final double speed;
        private final float startDistance;
        private final float stopDistance;
        @Nullable
        private ServerPlayerEntity owner;
        private int updateCountdownTicks;

        private FollowOwnerLikeWolfGoal(WitheringMinionEntity minion, double speed, float startDistance, float stopDistance) {
            this.minion = minion;
            this.speed = speed;
            this.startDistance = startDistance;
            this.stopDistance = stopDistance;
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            ServerPlayerEntity ownerPlayer = minion.getOwnerPlayer();
            if (ownerPlayer == null || ownerPlayer.isSpectator()) return false;
            if (minion.squaredDistanceTo(ownerPlayer) < startDistance * startDistance) return false;
            owner = ownerPlayer;
            return true;
        }

        @Override
        public boolean shouldContinue() {
            return owner != null
                    && owner.isAlive()
                    && !owner.isSpectator()
                    && minion.squaredDistanceTo(owner) > stopDistance * stopDistance;
        }

        @Override
        public void start() {
            updateCountdownTicks = 0;
        }

        @Override
        public void stop() {
            owner = null;
            minion.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (owner == null) return;
            minion.getLookControl().lookAt(owner, 10.0F, 10.0F);

            if (minion.shouldTeleportToOwner(owner) && minion.tryTeleportNearOwner(owner)) {
                updateCountdownTicks = 10;
                return;
            }

            if (--updateCountdownTicks <= 0) {
                updateCountdownTicks = 10;
                minion.getNavigation().startMovingTo(owner, speed);
            }
        }
    }
}
