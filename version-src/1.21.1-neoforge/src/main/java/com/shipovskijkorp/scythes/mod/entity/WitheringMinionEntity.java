package com.shipovskijkorp.scythes.mod.entity;

import com.shipovskijkorp.scythes.mod.ability.WitheringMinionManager;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import java.util.EnumSet;
import java.util.UUID;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class WitheringMinionEntity extends WitherSkeletonEntity {

    private static final String OWNER_KEY = "Owner";
    private static final String LIFE_TICKS_KEY = "LifeTicks";

    @Nullable
    private UUID ownerUuid;
    private int lifeTicks;
    private long lastDamageWorldTick;
    @Nullable
    private LivingEntity ownerDefenseTarget;
    private boolean targetStateInitialized;
    private int observedOwnerHurtTimestamp;
    @Nullable
    private UUID observedOwnerAttackerUuid;
    private int observedSelfHurtTimestamp;
    @Nullable
    private UUID observedSelfAttackerUuid;
    private int observedOwnerAttackTimestamp;
    @Nullable
    private UUID observedOwnerAttackTargetUuid;

    public WitheringMinionEntity(EntityType<? extends WitheringMinionEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 0;
        this.setCanPickUpLoot(false);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, ScytheBalance.Minion.MAX_HEALTH)
                .add(EntityAttributes.GENERIC_ARMOR, ScytheBalance.Minion.ARMOR)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, ScytheBalance.Minion.MOVEMENT_SPEED)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, ScytheBalance.Minion.ATTACK_DAMAGE)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, ScytheBalance.Minion.FOLLOW_RANGE);
    }

    @Override
    public boolean isPushedByFluids() {
        return false;
    }

    @Override
    protected void initGoals() {
        // Summoned wither skeletons are land mobs. The amphibious navigation/move
        // controller used before this prevented normal one-block ground traversal
        // and caused repeated yaw corrections on land. Keep vanilla ground movement
        // and only add the ordinary swimming escape goal.
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, ScytheBalance.Minion.MELEE_SPEED, true));
        this.goalSelector.add(3, new FollowOwnerLikeWolfGoal(this, ScytheBalance.Minion.FOLLOW_SPEED, ScytheBalance.Minion.FOLLOW_START_DISTANCE, ScytheBalance.Minion.FOLLOW_STOP_DISTANCE));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, ScytheBalance.Minion.LOOK_DISTANCE));
        this.goalSelector.add(8, new LookAroundGoal(this));
    }

    public void initializeForOwner(ServerPlayerEntity owner) {
        this.ownerUuid = owner.getUuid();
        this.lifeTicks = 0;
        primeTargetState(owner);
        this.lastDamageWorldTick = getWorld().getTime();
        this.setCustomNameVisible(false);
        this.setPersistent();
        this.setHealth(ScytheBalance.Minion.MAX_HEALTH);
        setAttributeBase(EntityAttributes.GENERIC_MAX_HEALTH, ScytheBalance.Minion.MAX_HEALTH);
        setAttributeBase(EntityAttributes.GENERIC_ARMOR, ScytheBalance.Minion.ARMOR);
        this.setHealth(ScytheBalance.Minion.MAX_HEALTH);
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
        ServerPlayerEntity owner = getOwnerPlayer();
        if (owner == null) {
            WitheringMinionManager.suspendOffline(this);
            return;
        }

        WitheringMinionManager.ensureRegistered(this);
        if (lifeTicks >= ScytheBalance.Minion.LIFETIME_TICKS) {
            WitheringMinionManager.returnMinion(owner, this);
            discard();
            return;
        }

        double returnDistanceSquared = ScytheBalance.Minion.SEARCH_RADIUS * ScytheBalance.Minion.SEARCH_RADIUS;
        if (owner.getWorld() != getWorld() || this.squaredDistanceTo(owner) > returnDistanceSquared) {
            WitheringMinionManager.returnMinion(owner, this);
            discard();
            return;
        }

        if (!targetStateInitialized) {
            primeTargetState(owner);
        }
        updateDogLikeTarget(owner);

        // Do this outside the follow goal as well. Combat owns the MOVE control at a
        // higher priority, so relying on the follow goal alone can leave a minion
        // chasing a stale target forever while its owner disappears into the distance.
        if (shouldTeleportToOwner(owner)) {
            tryTeleportNearOwner(owner);
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
        if (age % ScytheBalance.Minion.REGEN_INTERVAL_TICKS != 0) return;
        if (getHealth() >= getMaxHealth()) return;

        long now = getWorld().getTime();
        if (now - lastDamageWorldTick < ScytheBalance.Minion.REGEN_IDLE_TICKS) return;

        ServerPlayerEntity owner = getOwnerPlayer();
        if (owner == null) return;

        if (WitheringMinionManager.damageOwnerScytheForMinionRegen(owner, ScytheBalance.Minion.REGEN_DURABILITY_COST)) {
            heal(ScytheBalance.Minion.REGEN_HEALTH_PER_TICK);
        }
    }

    private void primeTargetState(ServerPlayerEntity owner) {
        targetStateInitialized = true;
        observedOwnerHurtTimestamp = owner.getLastAttackedTime();
        LivingEntity ownerAttacker = owner.getAttacker();
        observedOwnerAttackerUuid = ownerAttacker == null ? null : ownerAttacker.getUuid();
        observedSelfHurtTimestamp = getLastAttackedTime();
        LivingEntity selfAttacker = getAttacker();
        observedSelfAttackerUuid = selfAttacker == null ? null : selfAttacker.getUuid();
        observedOwnerAttackTimestamp = owner.getLastAttackTime();
        LivingEntity ownerTarget = owner.getAttacking();
        observedOwnerAttackTargetUuid = ownerTarget == null ? null : ownerTarget.getUuid();
        ownerDefenseTarget = null;
    }

    private void updateDogLikeTarget(ServerPlayerEntity owner) {
        int ownerHurtTimestamp = owner.getLastAttackedTime();
        LivingEntity ownerAttacker = owner.getAttacker();
        UUID ownerAttackerUuid = ownerAttacker == null ? null : ownerAttacker.getUuid();
        boolean newOwnerHit = ownerHurtTimestamp != observedOwnerHurtTimestamp
                || ownerAttackerUuid != null && !ownerAttackerUuid.equals(observedOwnerAttackerUuid);
        if (newOwnerHit) {
            observedOwnerHurtTimestamp = ownerHurtTimestamp;
            if (ownerAttackerUuid != null) {
                observedOwnerAttackerUuid = ownerAttackerUuid;
            }
            if (isAllowedTarget(owner, ownerAttacker)) {
                ownerDefenseTarget = ownerAttacker;
                setTarget(ownerDefenseTarget);
                return;
            }
            ownerDefenseTarget = null;
        }

        // The newest entity that actually hurt the owner remains the top-priority
        // threat until it dies/becomes invalid or the minion has to regroup.
        if (isAllowedTarget(owner, ownerDefenseTarget)) {
            if (getTarget() != ownerDefenseTarget) {
                setTarget(ownerDefenseTarget);
            }
            return;
        }
        ownerDefenseTarget = null;

        // Do not rescan and switch between several angry mobs every tick. If the
        // current target is actively targeting this minion, keep it; otherwise pick
        // the nearest mob that has already decided to attack us, before it lands a hit.
        LivingEntity current = getTarget();
        if (isAllowedTarget(owner, current) && current instanceof MobEntity mob && mob.getTarget() == this) {
            return;
        }
        LivingEntity incomingAggressor = findMobTargetingMe(owner);
        if (incomingAggressor != null) {
            setTarget(incomingAggressor);
            return;
        }

        int selfHurtTimestamp = getLastAttackedTime();
        LivingEntity selfAttacker = getAttacker();
        UUID selfAttackerUuid = selfAttacker == null ? null : selfAttacker.getUuid();
        boolean newSelfHit = selfHurtTimestamp != observedSelfHurtTimestamp
                || selfAttackerUuid != null && !selfAttackerUuid.equals(observedSelfAttackerUuid);
        if (newSelfHit) {
            observedSelfHurtTimestamp = selfHurtTimestamp;
            if (selfAttackerUuid != null) {
                observedSelfAttackerUuid = selfAttackerUuid;
            }
            if (isAllowedTarget(owner, selfAttacker)) {
                setTarget(selfAttacker);
                return;
            }
        }

        current = getTarget();
        if (isAllowedTarget(owner, current)) {
            return;
        }

        int ownerAttackTimestamp = owner.getLastAttackTime();
        LivingEntity ownerTarget = owner.getAttacking();
        UUID ownerTargetUuid = ownerTarget == null ? null : ownerTarget.getUuid();
        boolean newOwnerAttack = ownerAttackTimestamp != observedOwnerAttackTimestamp
                || ownerTargetUuid != null && !ownerTargetUuid.equals(observedOwnerAttackTargetUuid);
        if (newOwnerAttack) {
            observedOwnerAttackTimestamp = ownerAttackTimestamp;
            if (ownerTargetUuid != null) {
                observedOwnerAttackTargetUuid = ownerTargetUuid;
            }
            if (isAllowedTarget(owner, ownerTarget)) {
                setTarget(ownerTarget);
                return;
            }
        }

        setTarget(null);
    }

    @Nullable
    private LivingEntity findMobTargetingMe(ServerPlayerEntity owner) {
        Box searchBox = getBoundingBox().expand(ScytheBalance.Minion.FOLLOW_RANGE);
        LivingEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (MobEntity mob : getWorld().getEntitiesByClass(
                MobEntity.class,
                searchBox,
                mob -> mob != this && mob.getTarget() == this && isAllowedTarget(owner, mob)
        )) {
            double distance = squaredDistanceTo(mob);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = mob;
            }
        }

        return nearest;
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

    @Nullable
    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public int getLifeTicks() {
        return lifeTicks;
    }

    public void restoreRuntimeState(float health, int restoredLifeTicks) {
        lifeTicks = Math.max(0, restoredLifeTicks);
        setHealth(Math.max(0.1F, Math.min(getMaxHealth(), health)));
    }

    @Override
    protected boolean isAffectedByDaylight() {
        return false;
    }

    private boolean shouldTeleportToOwner(ServerPlayerEntity owner) {
        return this.squaredDistanceTo(owner) >= ScytheBalance.Minion.OWNER_TELEPORT_DISTANCE_SQUARED;
    }

    private boolean tryTeleportNearOwner(ServerPlayerEntity owner) {
        int ownerX = MathHelper.floor(owner.getX());
        int ownerY = MathHelper.floor(owner.getY());
        int ownerZ = MathHelper.floor(owner.getZ());

        for (int i = 0; i < ScytheBalance.Minion.CLASSIC_TELEPORT_ATTEMPTS; i++) {
            int dx = this.getRandom().nextInt(2 * ScytheBalance.Minion.CLASSIC_TELEPORT_RADIUS + 1) - ScytheBalance.Minion.CLASSIC_TELEPORT_RADIUS;
            int dy = this.getRandom().nextInt(2 * ScytheBalance.Minion.CLASSIC_TELEPORT_VERTICAL_RADIUS + 1) - ScytheBalance.Minion.CLASSIC_TELEPORT_VERTICAL_RADIUS;
            int dz = this.getRandom().nextInt(2 * ScytheBalance.Minion.CLASSIC_TELEPORT_RADIUS + 1) - ScytheBalance.Minion.CLASSIC_TELEPORT_RADIUS;

            if (Math.abs(dx) < ScytheBalance.Minion.TELEPORT_MIN_OFFSET && Math.abs(dz) < ScytheBalance.Minion.TELEPORT_MIN_OFFSET) {
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
        this.ownerDefenseTarget = null;
        this.setTarget(null);
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
            this.setControls(EnumSet.of(Control.MOVE));
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
            if (minion.shouldTeleportToOwner(owner) && minion.tryTeleportNearOwner(owner)) {
                updateCountdownTicks = ScytheBalance.Minion.AI_UPDATE_INTERVAL_TICKS;
                return;
            }

            if (minion.getNavigation().isIdle() || --updateCountdownTicks <= 0) {
                updateCountdownTicks = ScytheBalance.Minion.AI_UPDATE_INTERVAL_TICKS;
                minion.getNavigation().startMovingTo(owner, speed);
            }
        }
    }
}
