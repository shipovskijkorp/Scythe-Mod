package com.shipovskijkorp.scythes.mod.util;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
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
}
