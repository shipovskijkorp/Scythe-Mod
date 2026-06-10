package com.shipovskijkorp.scythes.mod.network;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import net.minecraft.util.Identifier;

public final class ModPackets {

    private ModPackets() {}

    /* ===================== C2S ===================== */

    public static final Identifier SCYTHE_ABILITY_C2S =
            new Identifier(ScytheMod.MOD_ID, "scythe_ability");

    /* ===================== S2C ===================== */

    public static final Identifier BLOOD_HARVEST_START_S2C =
            new Identifier(ScytheMod.MOD_ID, "blood_harvest_start");

    public static final Identifier BLOOD_HARVEST_STOP_S2C =
            new Identifier(ScytheMod.MOD_ID, "blood_harvest_stop");
}
