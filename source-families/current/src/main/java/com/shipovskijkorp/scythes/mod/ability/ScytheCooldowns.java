package com.shipovskijkorp.scythes.mod.ability;

import net.minecraft.server.level.ServerPlayer;

/** Small MC adapter: UUID and clock are the only version-dependent operations. */
public final class ScytheCooldowns {
    public enum Skill { BLENDER, TOXIC_ORB, TOXIC_AURA, WITHERING_AURA, MIDAS, RAIN, ICE_SPIKE, STORM, FARMER_HARVEST, FARMER_GROWTH, FIREBALL, FIRE_BURST }
    private static final CooldownStore<Skill> STORE = new CooldownStore<>();
    private ScytheCooldowns() {}

    public static int remaining(ServerPlayer player, Skill skill) {
        return STORE.remaining(player.getUUID(), skill, player.level().getGameTime());
    }

    public static void start(ServerPlayer player, Skill skill, int ticks) {
        STORE.start(player.getUUID(), skill, player.level().getGameTime(), ticks);
    }

    public static void clear(ServerPlayer player) { STORE.clear(player.getUUID()); }
    public static void clearAll() { STORE.clearAll(); }
}
