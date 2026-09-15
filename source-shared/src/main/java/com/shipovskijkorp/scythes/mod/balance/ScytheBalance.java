package com.shipovskijkorp.scythes.mod.balance;

/**
 * Single source of gameplay tuning for every Minecraft version and loader.
 * Distances are blocks, durations are game ticks, probabilities are in [0, 1],
 * health/damage use health points (two points = one heart). Effect amplifiers
 * are zero-based. Technical indices, wire IDs, rendering colors and geometry
 * constants are deliberately not balance settings.
 *
 * This class has no Minecraft or loader dependencies. Keep fields as primitive
 * compile-time constants; resource generation reads the same expressions.
 */
public final class ScytheBalance {
    private ScytheBalance() {}

    public static final int TICKS_PER_SECOND = 20;

    /** Common sword-like material and base stats. Damage includes the player base of 1. */
    public static final class Base {
        private Base() {}

        public static final int DURABILITY = 2031;
        public static final int ENCHANTABILITY = 15;
        public static final float MINING_SPEED = 9.0F;
        public static final int MINING_LEVEL = 4;
        public static final float MATERIAL_ATTACK_DAMAGE = 4.0F;
        public static final int ATTACK_DAMAGE_BONUS = 4;
        public static final float ATTACK_SPEED_MODIFIER = -2.8F;
        public static final float PLAYER_ATTACK_DAMAGE = 1.0F;
        public static final float PLAYER_ATTACK_SPEED = 4.0F;
        public static final float ATTACK_DAMAGE = PLAYER_ATTACK_DAMAGE + MATERIAL_ATTACK_DAMAGE + ATTACK_DAMAGE_BONUS;
        public static final float ATTACK_SPEED = PLAYER_ATTACK_SPEED + ATTACK_SPEED_MODIFIER;
        public static final int MAX_STACK_SIZE = 1;
        public static final boolean FIRE_RESISTANT = true;
        public static final int ACTIVE_DURABILITY_COST = 100;
    }

    /** Blood tuning. */
    public static final class Blood {
        private Blood() {}

        public static final double BLEEDING_CHANCE = 0.40D;
        public static final double DEFENSE_PIERCE_CHANCE = 0.20D;
        public static final double DEFENSE_PIERCE_MITIGATION_IGNORED = 0.50D;
        public static final int BLEEDING_BASE_DURATION_TICKS = TICKS_PER_SECOND * 2;
        public static final int BLEEDING_EXTEND_TICKS = TICKS_PER_SECOND * 2;
        public static final double BLENDER_RADIUS = 3.0D;
        public static final int BLENDER_COOLDOWN_TICKS = TICKS_PER_SECOND * 30;
        public static final int BLENDER_DURABILITY_COST = 50;
        public static final int BLENDER_HIT_COUNT = 2;
        public static final int BLENDER_SLOWNESS_TICKS = TICKS_PER_SECOND * 4;
        public static final int BLENDER_SLOWNESS_AMPLIFIER = 1;
        public static final int BLENDER_BLEEDING_TICKS = TICKS_PER_SECOND * 10;
        public static final int BLENDER_BLEEDING_AMPLIFIER = 1;
        public static final float BLENDER_SINGLE_HIT_DAMAGE = Base.ATTACK_DAMAGE;
        public static final double BLENDER_PULL_BASE = 0.45D;
        public static final double BLENDER_PULL_PER_BLOCK = 0.18D;
        public static final double BLENDER_PULL_Y = 0.18D;
        public static final int BLEEDING_AMPLIFIER = 0;
    }

    /** Active skill and success/failure outcomes. Effect amplifiers are zero-based. */
    public static final class BloodHarvest {
        private BloodHarvest() {}

        public static final int KILL_WINDOW_TICKS = TICKS_PER_SECOND * 20;
        public static final int SUCCESS_BUFF_TICKS = TICKS_PER_SECOND * 16;
        public static final int SUCCESS_SPEED_AMPLIFIER = 1;
        public static final int SUCCESS_STRENGTH_AMPLIFIER = 1;
        public static final int SUCCESS_REGEN_AMPLIFIER = 1;
        public static final int FAILURE_DEBUFF_TICKS = 150;
        public static final int FAILURE_SLOWNESS_AMPLIFIER = 1;
        public static final int FAILURE_WEAKNESS_AMPLIFIER = 1;
        public static final double RADIUS = 10.0D;
        public static final int COOLDOWN_TICKS = TICKS_PER_SECOND * 60;
        public static final int DURABILITY_COST = Base.ACTIVE_DURABILITY_COST;
        public static final int SLOWNESS_TICKS = TICKS_PER_SECOND * 10;
        public static final int BLINDNESS_TICKS = TICKS_PER_SECOND * 10;
        public static final int WEAKNESS_TICKS = TICKS_PER_SECOND * 10;
        public static final int GLOWING_TICKS = TICKS_PER_SECOND * 20;
        public static final int SLOWNESS_AMPLIFIER = 1;
        public static final int WEAKNESS_AMPLIFIER = 1;
        public static final int BLINDNESS_AMPLIFIER = 0;
        public static final int GLOWING_AMPLIFIER = 0;
        public static final int FAILURE_BLINDNESS_AMPLIFIER = 0;
    }

    /** Vampirism tuning. */
    public static final class Vampirism {
        private Vampirism() {}

        public static final double VAMPIRISM_CHANCE = 0.25D;
        public static final double HEAL_FRACTION = 0.50D;
        public static final int COOLDOWN_TICKS = 20;
    }

    /** Bleeding tuning. */
    public static final class Bleeding {
        private Bleeding() {}

        public static final int TICK_RATE = 20;
        public static final double DAMAGE_PER_SECOND = 1.5D;
    }

    /** Toxic tuning. */
    public static final class Toxic {
        private Toxic() {}

        public static final double PASSIVE_ARMOR_DAMAGE_CHANCE = 0.30D;
        public static final int PASSIVE_ARMOR_DAMAGE = 20;
        public static final double PASSIVE_POISON_CHANCE = 0.25D;
        public static final int PASSIVE_POISON_TICKS = TICKS_PER_SECOND * 2;
        public static final int PASSIVE_POISON_AMPLIFIER = 1;
        public static final double ACIDITY_ARMOR_DAMAGE_BONUS_PER_LEVEL = 0.11D;
        public static final int ORB_COOLDOWN_TICKS = TICKS_PER_SECOND * 5;
        public static final int ORB_DURABILITY_COST = 20;
        public static final float ORB_SPEED = 1.5F;
    }

    /** Probabilities and armor wear are PER GAME TICK, not per second. */
    public static final class ToxicAura {
        private ToxicAura() {}

        public static final int AURA_COOLDOWN_TICKS = TICKS_PER_SECOND * 60;
        public static final int AURA_DURABILITY_COST = Base.ACTIVE_DURABILITY_COST;
        public static final int DURATION_TICKS = TICKS_PER_SECOND * 10;
        public static final double RADIUS = 5.0D;
        public static final int POISON_TICKS = TICKS_PER_SECOND * 10;
        public static final int POISON_AMPLIFIER = 1;
        public static final double PURE_DAMAGE_CHANCE = 0.20D;
        public static final float PURE_DAMAGE = 1.0F;
        public static final int ARMOR_DAMAGE_PER_TICK = 1;
        public static final double NAUSEA_CHANCE = 0.15D;
        public static final int NAUSEA_TICKS = TICKS_PER_SECOND * 10;
        public static final int NAUSEA_AMPLIFIER = 0;
    }

    /** ToxicOrb tuning. */
    public static final class ToxicOrb {
        private ToxicOrb() {}

        public static final double DAMAGE_RADIUS = 2.0D;
        public static final float PURE_DAMAGE = 2.0F;
        public static final int POISON_TICKS = TICKS_PER_SECOND * 10;
        public static final int POISON_AMPLIFIER = 1;
        public static final int ARMOR_DAMAGE = 30;
        public static final int MAX_LIFETIME_TICKS = TICKS_PER_SECOND * 8;
        public static final double SPAWN_EYE_OFFSET = 0.15D;
        public static final float WIDTH = 0.35F;
        public static final float HEIGHT = 0.35F;
        public static final int TRACKING_RANGE = 4;
        public static final int UPDATE_INTERVAL = 10;
    }

    /** Withering tuning. */
    public static final class Withering {
        private Withering() {}

        public static final double WITHER_CHANCE = 0.30D;
        public static final int WITHER_TICKS = TICKS_PER_SECOND * 10;
        public static final int WITHER_AMPLIFIER = 1;
        public static final int SOULS_PER_MOB_KILL = 1;
        public static final int SOULS_PER_PLAYER_KILL = 12;
        public static final int MINION_DURABILITY_COST = 10;
        public static final int MINION_SOUL_COST = 6;
        public static final int MAX_MINIONS = 6;
        public static final double SOUL_SIPHON_CHANCE = 0.50D;
        public static final int SOUL_SIPHON_MULTIPLIER = 2;
        public static final int ADDITIONAL_SLOT_LEVEL_ONE = 1;
        public static final int ADDITIONAL_SLOT_LEVEL_TWO = 2;
        public static final int ADDITIONAL_SLOT_LEVEL_THREE = 4;
    }

    /** WitheringAura tuning. */
    public static final class WitheringAura {
        private WitheringAura() {}

        public static final int AURA_COOLDOWN_TICKS = TICKS_PER_SECOND * 60;
        public static final int AURA_DURABILITY_COST = Base.ACTIVE_DURABILITY_COST;
        public static final int DURATION_TICKS = TICKS_PER_SECOND * 10;
        public static final double WITHER_RADIUS = 5.0D;
        public static final int WITHER_TICKS = TICKS_PER_SECOND * 10;
        public static final int WITHER_AMPLIFIER = 1;
        public static final double MINION_BUFF_RADIUS = 10.0D;
        public static final int MINION_BUFF_TICKS = TICKS_PER_SECOND * 3;
        public static final int MINION_BUFF_AMPLIFIER = 1;
    }

    /** Minion attributes, regeneration, AI ranges and safe teleport search. */
    public static final class Minion {
        private Minion() {}

        public static final double OWNER_TELEPORT_DISTANCE = 12.0D;
        public static final float MAX_HEALTH = 20.0F;
        public static final double ARMOR = 14.0D;
        public static final int LIFETIME_TICKS = TICKS_PER_SECOND * 60 * 15;
        public static final int REGEN_IDLE_TICKS = TICKS_PER_SECOND * 5;
        public static final int REGEN_INTERVAL_TICKS = 20;
        public static final float REGEN_HEALTH_PER_TICK = 1.0F;
        public static final int REGEN_DURABILITY_COST = 1;
        public static final double OWNER_TELEPORT_DISTANCE_SQUARED = OWNER_TELEPORT_DISTANCE * OWNER_TELEPORT_DISTANCE;
        public static final double SEARCH_RADIUS = 160.0D;
        public static final double MOVEMENT_SPEED = 0.25D;
        public static final double ATTACK_DAMAGE = 4.0D;
        public static final double FOLLOW_RANGE = 32.0D;
        public static final double MELEE_SPEED = 1.2D;
        public static final double FOLLOW_SPEED = 1.15D;
        public static final float FOLLOW_START_DISTANCE = 5.0F;
        public static final float FOLLOW_STOP_DISTANCE = 2.5F;
        public static final double WANDER_SPEED = 0.8D;
        public static final float LOOK_DISTANCE = 8.0F;
        public static final int AI_UPDATE_INTERVAL_TICKS = 10;
        public static final int SWIM_PITCH_CHANGE = 85;
        public static final int SWIM_YAW_CHANGE = 10;
        public static final float SWIM_WATER_SPEED_MULTIPLIER = 0.8F;
        public static final float SWIM_LAND_SPEED_MULTIPLIER = 1.0F;
        // Classic random-position search is retained for 1.20/1.21 targets.
        public static final int CLASSIC_TELEPORT_ATTEMPTS = 10;
        public static final int CLASSIC_TELEPORT_RADIUS = 3;
        public static final int CLASSIC_TELEPORT_VERTICAL_RADIUS = 1;
        // 26.x uses a wider search with a vertical scan instead.
        public static final int TELEPORT_ATTEMPTS = 48;
        public static final int TELEPORT_RADIUS = 5;
        public static final int TELEPORT_MIN_OFFSET = 2;
        public static final int TELEPORT_SCAN_UP = 3;
        public static final int TELEPORT_SCAN_DOWN = 5;
        public static final double SPAWN_DISTANCE = 1.6D;
        public static final float WIDTH = 0.7F;
        public static final float HEIGHT = 2.4F;
        public static final int TRACKING_RANGE = 8;
        public static final int UPDATE_INTERVAL = 3;
    }

    /** Golden tuning. */
    public static final class Golden {
        private Golden() {}

        public static final int PASSIVE_LOOTING_BONUS = 1;
        public static final int MIDAS_COOLDOWN_TICKS = TICKS_PER_SECOND * 40;
        public static final int MIDAS_DURABILITY_COST = 50;
        public static final float MIDAS_DAMAGE_FRACTION = 0.50F;
        public static final float MIDAS_MIN_DAMAGE = 20.0F;
        public static final float MIDAS_MAX_DAMAGE = 100.0F;
        public static final double MIDAS_REACH = 5.0D;
        public static final int MARK_LOOTING_BONUS = 2;
        public static final double MIDAS_SEARCH_PADDING = 1.0D;
        public static final double MIDAS_HITBOX_PADDING = 0.35D;
    }

    /** GoldenRain tuning. */
    public static final class GoldenRain {
        private GoldenRain() {}

        public static final double RADIUS = 20.0D;
        public static final int COOLDOWN_TICKS = TICKS_PER_SECOND * 150;
        public static final int DURABILITY_COST = Base.ACTIVE_DURABILITY_COST;
    }

    /** Frozen tuning. */
    public static final class Frozen {
        private Frozen() {}

        public static final int ICE_SPIKE_DURABILITY_COST = 30;
        public static final int ICE_SPIKE_COOLDOWN_TICKS = TICKS_PER_SECOND * 20;
        public static final float ICE_SPIKE_SPEED = 3.0F;
        public static final double COLD_MASTER_FREEZING_CHANCE = 0.20D;
        public static final int COLD_MASTER_FREEZING_TICKS = 20;
        public static final int COLD_MASTER_FROST_WALKER_LEVEL = 2;
        public static final int FROST_WALKER_MAX_RADIUS = 16;
        public static final int FROST_WALKER_BASE_RADIUS = 2;
        public static final int FROSTED_ICE_MIN_DELAY_TICKS = 60;
        public static final int FROSTED_ICE_MAX_DELAY_TICKS = 120;
        public static final int COLD_MASTER_FREEZING_AMPLIFIER = 0;
    }

    /** FrozenStorm tuning. */
    public static final class FrozenStorm {
        private FrozenStorm() {}

        public static final double RADIUS = 15.0D;
        public static final int COOLDOWN_TICKS = TICKS_PER_SECOND * 40;
        public static final int DURABILITY_COST = Base.ACTIVE_DURABILITY_COST;
        public static final int FREEZING_TICKS = TICKS_PER_SECOND * 5;
        public static final int SLOWNESS_TICKS = TICKS_PER_SECOND * 10;
        public static final int WEAKNESS_TICKS = TICKS_PER_SECOND * 10;
        public static final int SLOWNESS_AMPLIFIER = 0;
        public static final int WEAKNESS_AMPLIFIER = 0;
        public static final int FREEZING_AMPLIFIER = 0;
    }

    /** IceSpike tuning. */
    public static final class IceSpike {
        private IceSpike() {}

        public static final float HIT_DAMAGE = 10.0F;
        public static final int FREEZING_TICKS = 30;
        public static final int MAX_LIFETIME_TICKS = TICKS_PER_SECOND * 10;
        public static final int FREEZING_AMPLIFIER = 0;
        public static final float WIDTH = 0.5F;
        public static final float HEIGHT = 0.5F;
        public static final int TRACKING_RANGE = 8;
        public static final int UPDATE_INTERVAL = 10;
    }

    /** Freezing tuning. */
    public static final class Freezing {
        private Freezing() {}

        public static final int DAMAGE_INTERVAL_TICKS = 20;
        public static final float DAMAGE_PER_PROC = 1.0F;
        public static final float DAMAGE_CHANCE = 0.60F;
    }

    /** FrozenHeart tuning. */
    public static final class FrozenHeart {
        private FrozenHeart() {}

        public static final int FREEZING_DURATION_TICKS = 3 * TICKS_PER_SECOND;
        public static final int NUTRITION = 3;
        public static final float SATURATION_MODIFIER = 0.3F;
        public static final int FREEZING_AMPLIFIER = 0;
    }


    /** Fire Scythe passive, lock-on projectile and lava mobility tuning. */
    public static final class Fire {
        private Fire() {}

        public static final double PASSIVE_BURNS_CHANCE = 0.20D;
        public static final int PASSIVE_BURNS_TICKS = TICKS_PER_SECOND * 3;
        public static final int PASSIVE_REFRESH_THRESHOLD_TICKS = TICKS_PER_SECOND;
        public static final int PASSIVE_FIRE_SECONDS = 4;

        public static final int FIREBALL_CHARGE_TICKS = TICKS_PER_SECOND;
        public static final int FIREBALL_COOLDOWN_TICKS = TICKS_PER_SECOND * 25;
        public static final int FIREBALL_DURABILITY_COST = 50;
        public static final double FIREBALL_LOCK_RANGE = 128.0D;
        public static final double FIREBALL_LOCK_MIN_DOT = 0.965D;
        public static final float FIREBALL_SPEED = 1.45F;
        public static final double FIREBALL_HOMING_STRENGTH = 0.18D;
        public static final int FIREBALL_MAX_LIFETIME_TICKS = TICKS_PER_SECOND * 15;
        public static final double FIREBALL_EXPLOSION_RADIUS = 4.0D;
        public static final float FIREBALL_MAX_DAMAGE = 10.0F;
        public static final int FIREBALL_FIRE_SECONDS = 5;
        public static final int FIREBALL_BURNS_TICKS = TICKS_PER_SECOND * 3;
        public static final int FIREBALL_STUN_TICKS = 10;
        public static final float FIREBALL_WIDTH = 0.35F;
        public static final float FIREBALL_HEIGHT = 0.35F;
        public static final int FIREBALL_TRACKING_RANGE = 16;
        public static final int FIREBALL_UPDATE_INTERVAL = 2;
        public static final double FIREBALL_SPAWN_EYE_OFFSET = 0.15D;
    }

    /** Fire Scythe area ability. */
    public static final class FireBurst {
        private FireBurst() {}

        public static final double RADIUS = 10.0D;
        public static final int COOLDOWN_TICKS = TICKS_PER_SECOND * 45;
        public static final int DURABILITY_COST = Base.ACTIVE_DURABILITY_COST;
        public static final int ARMOR_DAMAGE = 30;
        public static final int FIRE_SECONDS = 6;
        public static final int BURNS_TICKS = TICKS_PER_SECOND * 5;
        public static final int LAUNCH_DELAY_TICKS = TICKS_PER_SECOND;
        public static final double LAUNCH_VELOCITY_Y = 0.90D;
    }

    /** Burns anti-heal and hidden immunity rules. */
    public static final class Burns {
        private Burns() {}

        public static final int MAX_CONTINUOUS_TICKS = TICKS_PER_SECOND * 6;
        public static final int IMMUNITY_TICKS = TICKS_PER_SECOND * 5;
        public static final int AMPLIFIER = 0;
    }

    /** Farmer Scythe tuning. Base tool stats follow the other scythes; combat damage follows a Netherite Hoe. */
    public static final class Farmer {
        private Farmer() {}

        /** Vanilla Netherite Hoe total attack damage, while attack speed deliberately stays at the scythe baseline. */
        public static final float ATTACK_DAMAGE = 1.0F;
        public static final float MATERIAL_ATTACK_DAMAGE = 4.0F;
        public static final float HOE_ATTACK_DAMAGE_BASELINE = ATTACK_DAMAGE - Base.PLAYER_ATTACK_DAMAGE - MATERIAL_ATTACK_DAMAGE;
        public static final float HOE_ATTACK_SPEED_BASELINE = Base.ATTACK_SPEED_MODIFIER;

        public static final double DOUBLE_DROP_CHANCE = 0.20D;

        /** Right-click hoeing expands to a square around the clicked tillable block. */
        public static final int TILLING_RADIUS = 1;
        public static final int TILLING_DIAMETER = TILLING_RADIUS * 2 + 1;

        public static final double MASS_HARVEST_RADIUS = 20.0D;
        public static final int MASS_HARVEST_COOLDOWN_TICKS = TICKS_PER_SECOND * 10;
        public static final int MASS_HARVEST_DURABILITY_COST = 50;

        public static final double GROWTH_ACCELERATION_RADIUS = 20.0D;
        public static final double GROWTH_REDUCTION_FRACTION = 0.15D;
        public static final int GROWTH_BONE_MEAL_COST = 32;
        public static final int GROWTH_DURABILITY_COST = Base.ACTIVE_DURABILITY_COST;
        public static final int GROWTH_COOLDOWN_TICKS = TICKS_PER_SECOND * 60;
    }

    /** Loot chances and output counts; vanilla Fortune/Looting rules are unchanged. */
    public static final class Drops {
        private Drops() {}

        public static final float HEART_CHANCE = 0.05F;
        public static final double VILLAGER_DROP_CHANCE = 0.05D;
        public static final double PLAYER_DROP_CHANCE = 0.20D;
        public static final int FROZEN_HEART_LOOT_ROLLS = 1;
        public static final int FROZEN_HEART_COUNT = 1;
        public static final int BLOODY_ESSENCE_COUNT = 1;
    }

    /** Also substituted into data-driven enchantment JSON during source materialization. */
    public static final class Enchantments {
        private Enchantments() {}

        public static final int SPIKED_BLADE_MAX_LEVEL = 3;
        public static final int SPIKED_BLADE_WEIGHT = 2;
        public static final int SPIKED_BLADE_MIN_COST = 11;
        public static final int SPIKED_BLADE_COST_PER_LEVEL = 10;
        public static final int SPIKED_BLADE_MAX_COST = 16;
        public static final int SPIKED_BLADE_ANVIL_COST = 4;
        public static final int ADDITIONAL_SLOT_MAX_LEVEL = 3;
        public static final int ADDITIONAL_SLOT_WEIGHT = 2;
        public static final int ADDITIONAL_SLOT_MIN_COST = 12;
        public static final int ADDITIONAL_SLOT_COST_PER_LEVEL = 15;
        public static final int ADDITIONAL_SLOT_COST_SPREAD = 35;
        public static final int ADDITIONAL_SLOT_ANVIL_COST = 4;
        public static final int SOUL_SIPHON_MAX_LEVEL = 1;
        public static final int SOUL_SIPHON_WEIGHT = 2;
        public static final int SOUL_SIPHON_MIN_COST = 20;
        public static final int SOUL_SIPHON_MAX_COST = 60;
        public static final int SOUL_SIPHON_ANVIL_COST = 4;
        public static final int ACIDITY_MAX_LEVEL = 3;
        public static final int ACIDITY_WEIGHT = 5;
        public static final int ACIDITY_MIN_COST = 8;
        public static final int ACIDITY_COST_PER_LEVEL = 12;
        public static final int ACIDITY_COST_SPREAD = 35;
        public static final int ACIDITY_ANVIL_COST = 2;
        public static final int ACIDITY_MAX_COST = ACIDITY_MIN_COST + ACIDITY_COST_SPREAD;
        public static final int ADDITIONAL_SLOT_MAX_COST = ADDITIONAL_SLOT_MIN_COST + ADDITIONAL_SLOT_COST_SPREAD;
        public static final int SOUL_SIPHON_COST_PER_LEVEL = 0;

    }

    /** Crafting tuning. */
    public static final class Crafting {
        private Crafting() {}

        public static final int TOXIC_ESSENCE_OUTPUT_COUNT = 1;
        public static final int FIRE_ESSENCE_OUTPUT_COUNT = 1;
    }

    /** Progression tuning. */
    public static final class Progression {
        private Progression() {}

        public static final int BLOOD_SKILL_EXPIRE_TICKS = TICKS_PER_SECOND * 120;
        public static final int TOXIC_POISON_REQUIRED_TICKS = TICKS_PER_SECOND * 22;
        public static final int MINION_ARMY_SIZE = 10;
    }

    /** Non-gameplay timings shared by every target. */
    public static final class Presentation {
        private Presentation() {}

        public static final long TAB_ICON_INTERVAL_MS = 1200L;
    }

}
