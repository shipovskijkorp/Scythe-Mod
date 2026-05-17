package com.shipovskijkorp.scythes.mod.ability;

import com.shipovskijkorp.scythes.mod.config.ScytheModConfigLoader;
import com.shipovskijkorp.scythes.mod.network.WitheringHudS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WitheringScytheTracker {

    private static final Map<UUID, Integer> ACTIVE = new HashMap<>();

    /**
     * Запускает активку и возвращает её длительность в тиках (для HUD).
     */
    public static int start(ServerPlayerEntity player) {
        int ticks = 20 * 10; // дефолт 10 секунд

        if (ScytheModConfigLoader.CONFIG != null) {
            ticks = Math.max(1, ScytheModConfigLoader.CONFIG.witheringActiveTicks);
        }

        ACTIVE.put(player.getUuid(), ticks);

        return ticks;
    }

    public static boolean isActive(ServerPlayerEntity player) {
        return ACTIVE.containsKey(player.getUuid());
    }

    /** ✅ СКОЛЬКО тиков осталось (нужно для HUD и синхры) */
    public static int getTicksLeft(ServerPlayerEntity player) {
        return ACTIVE.getOrDefault(player.getUuid(), 0);
    }

    /**
     * @return ticksLeft after tick, 0 если закончилось, -1 если неактивно
     */
    public static int tick(ServerPlayerEntity player) {
        UUID id = player.getUuid();
        Integer time = ACTIVE.get(id);
        if (time == null) return -1;

        // если владелец мёртв/в спектаторе — активка должна сразу прекратиться
        if (!player.isAlive() || player.isSpectator()) {
            ACTIVE.remove(id);
            WitheringHudS2CPacket.sendStop(player);
            return 0;
        }

        time--;
        if (time <= 0) {
            ACTIVE.remove(id);
            WitheringHudS2CPacket.sendStop(player);
            return 0;
        } else {
            ACTIVE.put(id, time);
            return time;
        }
    }

    /**
     * Удаляет активку без отправки пакетов (например, при DISCONNECT).
     */
    public static void clear(ServerPlayerEntity player) {
        ACTIVE.remove(player.getUuid());
    }
}
