package com.shipovskijkorp.scythes.mod.util;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public final class ScytheDamageTypes {

    public static final ResourceKey<DamageType> BLEEDING = ResourceKey.create(Registries.DAMAGE_TYPE, ScytheMod.id("bleeding"));
    public static final ResourceKey<DamageType> BLOOD_PIERCE = ResourceKey.create(Registries.DAMAGE_TYPE, ScytheMod.id("blood_pierce"));
    public static final ResourceKey<DamageType> MIDAS_TOUCH = ResourceKey.create(Registries.DAMAGE_TYPE, ScytheMod.id("midas_touch"));
    public static final ResourceKey<DamageType> FREEZING = ResourceKey.create(Registries.DAMAGE_TYPE, ScytheMod.id("freezing"));

    private ScytheDamageTypes() {}

    public static DamageSource bleeding(Level world) {
        return world.damageSources().source(BLEEDING);
    }

    public static DamageSource bleeding(Level world, Entity attacker) {
        return world.damageSources().source(BLEEDING, attacker, attacker);
    }

    public static DamageSource bloodPierce(Level world, Entity attacker) {
        return world.damageSources().source(BLOOD_PIERCE, attacker, attacker);
    }

    public static DamageSource midasTouch(Level world, Entity attacker) {
        return world.damageSources().source(MIDAS_TOUCH, attacker, attacker);
    }

    public static DamageSource freezing(Level world) {
        return world.damageSources().source(FREEZING);
    }

    public static DamageSource freezing(Level world, Entity attacker) {
        return world.damageSources().source(FREEZING, attacker, attacker);
    }

    public static DamageSource withering(Level world, Entity attacker) {
        return world.damageSources().source(DamageTypes.WITHER, attacker, attacker);
    }
}
