package com.shipovskijkorp.scythes.mod.util;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.World;

public final class ScytheDamageTypes {

    public static final RegistryKey<DamageType> BLEEDING = RegistryKey.of(
            RegistryKeys.DAMAGE_TYPE,
            ScytheMod.id("bleeding")
    );

    public static final RegistryKey<DamageType> BLOOD_PIERCE = RegistryKey.of(
            RegistryKeys.DAMAGE_TYPE,
            ScytheMod.id("blood_pierce")
    );

    public static final RegistryKey<DamageType> FREEZING = RegistryKey.of(
            RegistryKeys.DAMAGE_TYPE,
            ScytheMod.id("freezing")
    );

    public static final RegistryKey<DamageType> MIDAS_TOUCH = RegistryKey.of(
            RegistryKeys.DAMAGE_TYPE,
            ScytheMod.id("midas_touch")
    );

    private ScytheDamageTypes() {
    }

    public static DamageSource bleeding(World world) {
        return world.getDamageSources().create(BLEEDING);
    }

    public static DamageSource bleeding(World world, Entity attacker) {
        return world.getDamageSources().create(BLEEDING, attacker, attacker);
    }

    public static DamageSource bloodPierce(World world, Entity attacker) {
        return world.getDamageSources().create(BLOOD_PIERCE, attacker, attacker);
    }

    public static DamageSource freezing(World world) {
        return world.getDamageSources().create(FREEZING);
    }

    public static DamageSource freezing(World world, Entity attacker) {
        return world.getDamageSources().create(FREEZING, attacker, attacker);
    }

    public static DamageSource midasTouch(World world, Entity attacker) {
        return world.getDamageSources().create(MIDAS_TOUCH, attacker, attacker);
    }

    public static DamageSource withering(World world, Entity attacker) {
        return world.getDamageSources().create(DamageTypes.WITHER, attacker, attacker);
    }
}
