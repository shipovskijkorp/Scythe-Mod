package com.shipovskijkorp.scythes.mod.entity;

import com.shipovskijkorp.scythes.mod.ability.WitheringMinionManager;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import java.util.EnumSet;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

public class WitheringMinionEntity extends WitherSkeleton {

    private static final String OWNER_KEY = "Owner";
    private static final String LIFE_TICKS_KEY = "LifeTicks";
    private static final String LAST_DAMAGE_WORLD_TICK_KEY = "LastDamageWorldTick";

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

    public WitheringMinionEntity(EntityType<? extends WitheringMinionEntity> entityType, Level world) {
        super(entityType, world);
        this.xpReward = 0;
        this.moveControl = new SmoothSwimmingMoveControl(
                this,
                ScytheBalance.Minion.SWIM_PITCH_CHANGE,
                ScytheBalance.Minion.SWIM_YAW_CHANGE,
                ScytheBalance.Minion.SWIM_WATER_SPEED_MULTIPLIER,
                ScytheBalance.Minion.SWIM_LAND_SPEED_MULTIPLIER,
                true
        );
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, ScytheBalance.Minion.MAX_HEALTH)
                .add(Attributes.ARMOR, ScytheBalance.Minion.ARMOR)
                .add(Attributes.MOVEMENT_SPEED, ScytheBalance.Minion.MOVEMENT_SPEED)
                .add(Attributes.ATTACK_DAMAGE, ScytheBalance.Minion.ATTACK_DAMAGE)
                .add(Attributes.FOLLOW_RANGE, ScytheBalance.Minion.FOLLOW_RANGE);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new AmphibiousPathNavigation(this, level);
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, ScytheBalance.Minion.MELEE_SPEED, true));
        this.goalSelector.addGoal(3, new FollowOwnerLikeWolfGoal(this, ScytheBalance.Minion.FOLLOW_SPEED, ScytheBalance.Minion.FOLLOW_START_DISTANCE, ScytheBalance.Minion.FOLLOW_STOP_DISTANCE));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, ScytheBalance.Minion.WANDER_SPEED));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, ScytheBalance.Minion.LOOK_DISTANCE));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    public void initializeForOwner(ServerPlayer owner) {
        this.ownerUuid = owner.getUUID();
        this.lifeTicks = 0;
        primeTargetState(owner);
        this.lastDamageWorldTick = owner.level().getGameTime();
        this.setCustomNameVisible(false);
        this.setPersistenceRequired();
        setAttributeBase(Attributes.MAX_HEALTH, ScytheBalance.Minion.MAX_HEALTH);
        setAttributeBase(Attributes.ARMOR, ScytheBalance.Minion.ARMOR);
        this.setHealth(ScytheBalance.Minion.MAX_HEALTH);
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    private void setAttributeBase(Holder<Attribute> attribute, double value) {
        AttributeInstance instance = this.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (!(this.level() instanceof ServerLevel serverWorld)) {
            return;
        }

        lifeTicks++;
        ServerPlayer owner = getOwnerPlayer();
        if (lifeTicks >= ScytheBalance.Minion.LIFETIME_TICKS) {
            if (owner != null) {
                WitheringMinionManager.refundExpiredMinion(owner, this);
            }
            discard();
            return;
        }
        if (owner == null) {
            discard();
            return;
        }

        if (!targetStateInitialized) {
            primeTargetState(owner);
        }
        updateDogLikeTarget(owner);

        // Teleporting only from the follow goal is fragile: higher-priority combat goals
        // can hold the MOVE flag, so the minion keeps chasing a target instead of ever
        // running the owner-follow goal. Keep the dog-like teleport as a server-side
        // safety check in the entity tick so owned skeletons always regroup with their
        // player when they fall too far behind.
        if (shouldTeleportToOwner(owner)) {
            tryTeleportNearOwner(owner);
        }

        tryPassiveRegeneration(serverWorld);
    }

    @Override
    public boolean hurtServer(ServerLevel world, net.minecraft.world.damagesource.DamageSource source, float amount) {
        boolean damaged = super.hurtServer(world, source, amount);
        if (damaged && amount > 0.0F) {
            lastDamageWorldTick = world.getGameTime();
        }
        return damaged;
    }

    private void tryPassiveRegeneration(ServerLevel world) {
        if (tickCount % ScytheBalance.Minion.REGEN_INTERVAL_TICKS != 0) return;
        if (getHealth() >= getMaxHealth()) return;

        long now = world.getGameTime();
        if (now - lastDamageWorldTick < ScytheBalance.Minion.REGEN_IDLE_TICKS) return;

        ServerPlayer owner = getOwnerPlayer();
        if (owner == null) return;

        if (WitheringMinionManager.damageOwnerScytheForMinionRegen(owner, ScytheBalance.Minion.REGEN_DURABILITY_COST)) {
            heal(ScytheBalance.Minion.REGEN_HEALTH_PER_TICK);
        }
    }

    private void primeTargetState(ServerPlayer owner) {
        targetStateInitialized = true;
        observedOwnerHurtTimestamp = owner.getLastHurtByMobTimestamp();
        LivingEntity ownerAttacker = owner.getLastHurtByMob();
        observedOwnerAttackerUuid = ownerAttacker == null ? null : ownerAttacker.getUUID();
        observedSelfHurtTimestamp = getLastHurtByMobTimestamp();
        LivingEntity selfAttacker = getLastHurtByMob();
        observedSelfAttackerUuid = selfAttacker == null ? null : selfAttacker.getUUID();
        observedOwnerAttackTimestamp = owner.getLastHurtMobTimestamp();
        LivingEntity ownerTarget = owner.getLastHurtMob();
        observedOwnerAttackTargetUuid = ownerTarget == null ? null : ownerTarget.getUUID();
        ownerDefenseTarget = null;
    }

    private void updateDogLikeTarget(ServerPlayer owner) {
        int ownerHurtTimestamp = owner.getLastHurtByMobTimestamp();
        LivingEntity ownerAttacker = owner.getLastHurtByMob();
        UUID ownerAttackerUuid = ownerAttacker == null ? null : ownerAttacker.getUUID();
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

        // Keep an active aggressor instead of bouncing between every nearby mob that
        // happens to target this minion. New aggressors are picked before they hit.
        LivingEntity current = getTarget();
        if (isAllowedTarget(owner, current) && current instanceof Mob mob && mob.getTarget() == this) {
            return;
        }
        LivingEntity incomingAggressor = findMobTargetingMe(owner);
        if (incomingAggressor != null) {
            setTarget(incomingAggressor);
            return;
        }

        int selfHurtTimestamp = getLastHurtByMobTimestamp();
        LivingEntity selfAttacker = getLastHurtByMob();
        UUID selfAttackerUuid = selfAttacker == null ? null : selfAttacker.getUUID();
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

        int ownerAttackTimestamp = owner.getLastHurtMobTimestamp();
        LivingEntity ownerTarget = owner.getLastHurtMob();
        UUID ownerTargetUuid = ownerTarget == null ? null : ownerTarget.getUUID();
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
    private LivingEntity findMobTargetingMe(ServerPlayer owner) {
        AABB searchBox = getBoundingBox().inflate(ScytheBalance.Minion.FOLLOW_RANGE);
        LivingEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Mob mob : level().getEntitiesOfClass(
                Mob.class,
                searchBox,
                mob -> mob != this && mob.getTarget() == this && isAllowedTarget(owner, mob)
        )) {
            double distance = distanceToSqr(mob);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = mob;
            }
        }

        return nearest;
    }

    private boolean isAllowedTarget(ServerPlayer owner, @Nullable LivingEntity target) {
        if (target == null) return false;
        if (target instanceof WitheringMinionEntity minion && minion.isOwner(owner)) return false;
        return !com.shipovskijkorp.scythes.mod.util.ScytheCombatUtil.isInvalidHostileTarget(owner, target);
    }

    @Nullable
    public ServerPlayer getOwnerPlayer() {
        if (ownerUuid == null || !(this.level() instanceof ServerLevel serverWorld)) {
            return null;
        }
        return serverWorld.getServer().getPlayerList().getPlayer(ownerUuid);
    }

    public boolean isOwner(ServerPlayer player) {
        return ownerUuid != null && ownerUuid.equals(player.getUUID());
    }

    private boolean shouldTeleportToOwner(ServerPlayer owner) {
        return this.distanceToSqr(owner) >= ScytheBalance.Minion.OWNER_TELEPORT_DISTANCE_SQUARED;
    }

    private boolean tryTeleportNearOwner(ServerPlayer owner) {
        int ownerX = Mth.floor(owner.getX());
        int ownerY = Mth.floor(owner.getY());
        int ownerZ = Mth.floor(owner.getZ());

        for (int i = 0; i < ScytheBalance.Minion.TELEPORT_ATTEMPTS; i++) {
            int dx = this.getRandom().nextInt(2 * ScytheBalance.Minion.TELEPORT_RADIUS + 1) - ScytheBalance.Minion.TELEPORT_RADIUS;
            int dz = this.getRandom().nextInt(2 * ScytheBalance.Minion.TELEPORT_RADIUS + 1) - ScytheBalance.Minion.TELEPORT_RADIUS;

            if (Math.abs(dx) < ScytheBalance.Minion.TELEPORT_MIN_OFFSET && Math.abs(dz) < ScytheBalance.Minion.TELEPORT_MIN_OFFSET) {
                continue;
            }

            if (tryTeleportTo(ownerX + dx, ownerY, ownerZ + dz)) {
                return true;
            }
        }

        // Last resort: stand on the owner if every side spot is blocked. The safe-Y
        // scan below still prevents teleporting into solid blocks or liquids.
        return tryTeleportTo(ownerX, ownerY, ownerZ);
    }

    private boolean tryTeleportTo(int x, int ownerY, int z) {
        for (int dy = ScytheBalance.Minion.TELEPORT_SCAN_UP; dy >= -ScytheBalance.Minion.TELEPORT_SCAN_DOWN; dy--) {
            if (tryTeleportToExactY(x, ownerY + dy, z)) {
                return true;
            }
        }
        return false;
    }

    private boolean tryTeleportToExactY(int x, int y, int z) {
        BlockPos targetPos = new BlockPos(x, y, z);
        BlockPos groundPos = targetPos.below();

        if (!this.level().getBlockState(groundPos).isFaceSturdy(this.level(), groundPos, Direction.UP)) {
            return false;
        }
        if (!this.level().getBlockState(targetPos).getCollisionShape(this.level(), targetPos).isEmpty()
                || !this.level().getBlockState(targetPos.above()).getCollisionShape(this.level(), targetPos.above()).isEmpty()) {
            return false;
        }
        if (!this.level().getFluidState(targetPos).isEmpty() || !this.level().getFluidState(targetPos.above()).isEmpty()) {
            return false;
        }

        double targetX = x + 0.5D;
        double targetY = y;
        double targetZ = z + 0.5D;
        AABB targetBox = this.getBoundingBox().move(targetX - getX(), targetY - getY(), targetZ - getZ());
        if (!this.level().noCollision(this, targetBox)) {
            return false;
        }

        this.teleportTo(targetX, targetY, targetZ);
        this.setDeltaMovement(0.0D, 0.0D, 0.0D);
        this.getNavigation().stop();
        this.ownerDefenseTarget = null;
        this.setTarget(null);
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (ownerUuid != null) {
            output.putString(OWNER_KEY, ownerUuid.toString());
        }
        output.putInt(LIFE_TICKS_KEY, lifeTicks);
        output.putLong(LAST_DAMAGE_WORLD_TICK_KEY, lastDamageWorldTick);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        String owner = input.getStringOr(OWNER_KEY, "");
        if (!owner.isEmpty()) {
            try {
                ownerUuid = UUID.fromString(owner);
            } catch (IllegalArgumentException ignored) {
                ownerUuid = null;
            }
        } else {
            ownerUuid = null;
        }
        lifeTicks = Math.max(0, input.getIntOr(LIFE_TICKS_KEY, 0));
        lastDamageWorldTick = input.getLongOr(LAST_DAMAGE_WORLD_TICK_KEY, 0L);
    }

    private static final class FollowOwnerLikeWolfGoal extends Goal {
        private final WitheringMinionEntity minion;
        private final double speed;
        private final float startDistance;
        private final float stopDistance;
        @Nullable
        private ServerPlayer owner;
        private int updateCountdownTicks;

        private FollowOwnerLikeWolfGoal(WitheringMinionEntity minion, double speed, float startDistance, float stopDistance) {
            this.minion = minion;
            this.speed = speed;
            this.startDistance = startDistance;
            this.stopDistance = stopDistance;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            ServerPlayer ownerPlayer = minion.getOwnerPlayer();
            if (ownerPlayer == null || ownerPlayer.isSpectator()) return false;
            if (minion.distanceToSqr(ownerPlayer) < startDistance * startDistance) return false;
            owner = ownerPlayer;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return owner != null
                    && owner.isAlive()
                    && !owner.isSpectator()
                    && minion.distanceToSqr(owner) > stopDistance * stopDistance;
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
            minion.getLookControl().setLookAt(owner, 10.0F, 10.0F);

            if (minion.shouldTeleportToOwner(owner) && minion.tryTeleportNearOwner(owner)) {
                updateCountdownTicks = ScytheBalance.Minion.AI_UPDATE_INTERVAL_TICKS;
                return;
            }

            if (--updateCountdownTicks <= 0) {
                updateCountdownTicks = ScytheBalance.Minion.AI_UPDATE_INTERVAL_TICKS;
                minion.getNavigation().moveTo(owner, speed);
            }
        }
    }
}
