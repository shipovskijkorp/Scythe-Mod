package com.shipovskijkorp.scythes.mod.ability;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Loader- and Minecraft-independent cooldown storage; used on the server thread. */
public final class CooldownStore<K> {
    private final Map<UUID, Map<K, Long>> ready = new HashMap<>();

    public int remaining(UUID owner, K skill, long now) {
        Map<K, Long> skills = ready.get(owner);
        if (skills == null) return 0;
        Long readyAt = skills.get(skill);
        if (readyAt == null) return 0;
        if (readyAt <= now) {
            skills.remove(skill);
            if (skills.isEmpty()) ready.remove(owner);
            return 0;
        }
        long delta = readyAt - now;
        return delta < 0 || delta > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) delta;
    }

    public void start(UUID owner, K skill, long now, int ticks) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(skill, "skill");
        if (ticks <= 0) {
            Map<K, Long> skills = ready.get(owner);
            if (skills != null) {
                skills.remove(skill);
                if (skills.isEmpty()) ready.remove(owner);
            }
            return;
        }
        long deadline = now > Long.MAX_VALUE - ticks ? Long.MAX_VALUE : now + ticks;
        ready.computeIfAbsent(owner, ignored -> new HashMap<>()).put(skill, deadline);
    }

    public void clear(UUID owner) { ready.remove(owner); }
    public void clearAll() { ready.clear(); }
}
