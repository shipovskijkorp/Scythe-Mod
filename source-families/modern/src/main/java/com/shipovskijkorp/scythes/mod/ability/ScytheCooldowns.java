package com.shipovskijkorp.scythes.mod.ability;

import net.minecraft.server.network.ServerPlayerEntity;

/** Small MC adapter: UUID and clock are the only version-dependent operations. */
public final class ScytheCooldowns {
    public enum Skill { BLENDER, TOXIC_ORB, TOXIC_AURA, WITHERING_AURA, MIDAS, RAIN, ICE_SPIKE, STORM, FARMER_HARVEST, FARMER_GROWTH }
    private static final CooldownStore<Skill> STORE = new CooldownStore<>();
    private ScytheCooldowns() {}

    public static int remaining(ServerPlayerEntity player, Skill skill) {
//? if >=1.21.11 {
        return STORE.remaining(player.getUuid(), skill, player.getEntityWorld().getTime());
//? } else {
        return STORE.remaining(player.getUuid(), skill, player.getWorld().getTime());
//? }
    }

    public static void start(ServerPlayerEntity player, Skill skill, int ticks) {
//? if >=1.21.11 {
        STORE.start(player.getUuid(), skill, player.getEntityWorld().getTime(), ticks);
//? } else {
        STORE.start(player.getUuid(), skill, player.getWorld().getTime(), ticks);
//? }
    }

    public static void clear(ServerPlayerEntity player) { STORE.clear(player.getUuid()); }
    public static void clearAll() { STORE.clearAll(); }
}
