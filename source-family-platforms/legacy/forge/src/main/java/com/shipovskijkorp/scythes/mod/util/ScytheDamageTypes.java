package com.shipovskijkorp.scythes.mod.util;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public final class ScytheDamageTypes {

    public static final RegistryKey<DamageType> BLEEDING = RegistryKey.of(
            RegistryKeys.DAMAGE_TYPE,
            new Identifier(ScytheMod.MOD_ID, "bleeding")
    );

    public static final RegistryKey<DamageType> BLOOD_PIERCE = RegistryKey.of(
            RegistryKeys.DAMAGE_TYPE,
            new Identifier(ScytheMod.MOD_ID, "blood_pierce")
    );

    public static final RegistryKey<DamageType> MIDAS_TOUCH = RegistryKey.of(
            RegistryKeys.DAMAGE_TYPE,
            new Identifier(ScytheMod.MOD_ID, "midas_touch")
    );

    public static final RegistryKey<DamageType> FREEZING = RegistryKey.of(
            RegistryKeys.DAMAGE_TYPE,
            new Identifier(ScytheMod.MOD_ID, "freezing")
    );

    private ScytheDamageTypes() {
    }

    private static RegistryEntry<DamageType> type(World world, RegistryKey<DamageType> key) {
        return world.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(key);
    }

    public static DamageSource bleeding(World world) {
        return new DamageSource(type(world, BLEEDING));
    }

    public static DamageSource bleeding(World world, Entity attacker) {
        return new DamageSource(type(world, BLEEDING), attacker, attacker);
    }

    public static DamageSource bloodPierce(World world, Entity attacker) {
        return new DamageSource(type(world, BLOOD_PIERCE), attacker, attacker);
    }

    public static DamageSource midasTouch(World world, Entity attacker) {
        return new DamageSource(type(world, MIDAS_TOUCH), attacker, attacker);
    }

    public static DamageSource freezing(World world) {
        return new DamageSource(type(world, FREEZING));
    }

    public static DamageSource freezing(World world, Entity attacker) {
        return new DamageSource(type(world, FREEZING), attacker, attacker);
    }

    public static DamageSource withering(World world, Entity attacker) {
        return new DamageSource(type(world, DamageTypes.WITHER), attacker, attacker);
    }
}
