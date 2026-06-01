package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.config.ScytheModConfigLoader;
import com.shipovskijkorp.scythes.mod.network.PlagueHudS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlagueScytheTracker {

    private static final Map<UUID, Integer> ACTIVE = new HashMap<>();

    public static void clear(ServerPlayerEntity player) {
        ACTIVE.remove(player.getUuid());
    }

    public static void stop(ServerPlayerEntity player) {
        if (ACTIVE.remove(player.getUuid()) != null) {
            PlagueHudS2CPacket.sendStop(player);
        }
    }

    /** Сколько тиков осталось (для HUD/синхры) */
    public static int getTicksLeft(ServerPlayerEntity player) {
        return ACTIVE.getOrDefault(player.getUuid(), 0);
    }

    public static int start(ServerPlayerEntity player) {
        int ticks = 20 * 10;
        if (ScytheModConfigLoader.CONFIG != null) {
            ticks = Math.max(1, ScytheModConfigLoader.CONFIG.plagueActiveTicks);
        }
        ACTIVE.put(player.getUuid(), ticks);
        return ticks;
    }

    public static boolean isActive(ServerPlayerEntity player) {
        return ACTIVE.containsKey(player.getUuid());
    }

    /**
     * @return ticksLeft after tick, 0 если закончилось, -1 если неактивно
     */
    public static int tick(ServerPlayerEntity player) {
        UUID id = player.getUuid();
        Integer time = ACTIVE.get(id);
        if (time == null) return -1;

        if (!player.isAlive() || player.isSpectator()) {
            ACTIVE.remove(id);
            PlagueHudS2CPacket.sendStop(player);
            return 0;
        }

        time--;
        if (time <= 0) {
            ACTIVE.remove(id);
            PlagueHudS2CPacket.sendStop(player);
            return 0;
        }

        ACTIVE.put(id, time);
        return time;
    }
}
