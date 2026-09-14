package com.shipovskijkorp.scythes.mod.entity;

import com.shipovskijkorp.scythes.mod.ability.WitheringMinionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;

public class WitheringMinionEntity extends WitherSkeleton {

    private static final String OWNER_KEY = "Owner";
    private static final String LIFE_TICKS_KEY = "LifeTicks";
    private static final String LAST_DAMAGE_WORLD_TICK_KEY = "LastDamageWorldTick";

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

    public WitheringMinionEntity(EntityType<? extends WitheringMinionEntity> entityType, Level world) {
        super(entityType, world);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ARMOR, ARMOR)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.addGoal(3, new FollowOwnerLikeWolfGoal(this, 1.15D, 5.0F, 2.5F));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    public void initializeForOwner(ServerPlayer owner) {
        this.ownerUuid = owner.getUUID();
        this.lifeTicks = 0;
        this.lastDamageWorldTick = owner.level().getGameTime();
        this.setCustomNameVisible(false);
        this.setPersistenceRequired();
        setAttributeBase(Attributes.MAX_HEALTH, MAX_HEALTH);
        setAttributeBase(Attributes.ARMOR, ARMOR);
        this.setHealth(MAX_HEALTH);
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
        if (lifeTicks >= LIFETIME_TICKS || owner == null) {
            discard();
            return;
        }

        if (tickCount % 10 == 0) {
            updateDogLikeTarget();
        }

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
        if (tickCount % REGEN_INTERVAL_TICKS != 0) return;
        if (getHealth() >= getMaxHealth()) return;

        long now = world.getGameTime();
        if (now - lastDamageWorldTick < REGEN_IDLE_TICKS) return;

        ServerPlayer owner = getOwnerPlayer();
        if (owner == null) return;

        if (WitheringMinionManager.damageOwnerScytheForMinionRegen(owner, REGEN_DURABILITY_COST)) {
            heal(REGEN_HEALTH_PER_TICK);
        }
    }

    private void updateDogLikeTarget() {
        ServerPlayer owner = getOwnerPlayer();
        if (owner == null) return;

        LivingEntity current = getTarget();
        if (isAllowedTarget(owner, current)) {
            return;
        }

        LivingEntity selfAttacker = getLastHurtByMob();
        if (isAllowedTarget(owner, selfAttacker)) {
            setTarget(selfAttacker);
            return;
        }

        LivingEntity ownerAttacker = owner.getLastHurtByMob();
        if (isAllowedTarget(owner, ownerAttacker)) {
            setTarget(ownerAttacker);
            return;
        }

        LivingEntity ownerTarget = owner.getLastHurtMob();
        if (isAllowedTarget(owner, ownerTarget)) {
            setTarget(ownerTarget);
            return;
        }

        setTarget(null);
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
        return this.distanceToSqr(owner) >= OWNER_TELEPORT_DISTANCE_SQUARED;
    }

    private boolean tryTeleportNearOwner(ServerPlayer owner) {
        int ownerX = Mth.floor(owner.getX());
        int ownerY = Mth.floor(owner.getY());
        int ownerZ = Mth.floor(owner.getZ());

        for (int i = 0; i < 48; i++) {
            int dx = this.getRandom().nextInt(11) - 5;
            int dz = this.getRandom().nextInt(11) - 5;

            if (Math.abs(dx) < 2 && Math.abs(dz) < 2) {
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
        for (int dy = 3; dy >= -5; dy--) {
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
                updateCountdownTicks = 10;
                return;
            }

            if (--updateCountdownTicks <= 0) {
                updateCountdownTicks = 10;
                minion.getNavigation().moveTo(owner, speed);
            }
        }
    }
}
