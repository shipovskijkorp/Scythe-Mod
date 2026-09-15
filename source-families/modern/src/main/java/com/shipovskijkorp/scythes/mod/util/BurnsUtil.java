package com.shipovskijkorp.scythes.mod.util;

import com.shipovskijkorp.scythes.mod.ScytheMod;
import com.shipovskijkorp.scythes.mod.balance.ScytheBalance;
import java.util.WeakHashMap;
import java.util.Map;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;

/** Server-side Burns duration cap and hidden post-effect immunity. */
public final class BurnsUtil {
    private record State(long continuousStart, long nextAllowedAt) {}
    private static final Map<LivingEntity, State> STATES = new WeakHashMap<>();

    private BurnsUtil() {}

    public static boolean tryApplyPassive(LivingEntity target) {
        StatusEffectInstance existing = target.getStatusEffect(ScytheMod.BURNS);
        if (existing != null && existing.getDuration() > ScytheBalance.Fire.PASSIVE_REFRESH_THRESHOLD_TICKS) return false;
        return apply(target, ScytheBalance.Fire.PASSIVE_BURNS_TICKS);
    }

    public static boolean applyActive(LivingEntity target, int durationTicks) {
        StatusEffectInstance existing = target.getStatusEffect(ScytheMod.BURNS);
        if (existing != null && existing.getDuration() >= durationTicks) return false;
        return apply(target, durationTicks);
    }

    private static boolean apply(LivingEntity target, int requestedTicks) {
//? if >=1.21.11 {
        if (target.getEntityWorld().isClient() || requestedTicks <= 0) return false;
        long now = target.getEntityWorld().getTime();
//? } else {
        if (target.getWorld().isClient || requestedTicks <= 0) return false;
        long now = target.getWorld().getTime();
//? }
        StatusEffectInstance existing = target.getStatusEffect(ScytheMod.BURNS);
        State state = STATES.get(target);

        if (existing == null) {
            if (state != null && now < state.nextAllowedAt()) return false;
            state = new State(now, now);
        } else if (state == null || now < state.continuousStart()) {
            state = new State(now, now);
        }

        long continuousEnd = state.continuousStart() + ScytheBalance.Burns.MAX_CONTINUOUS_TICKS;
        int allowedTicks = (int) Math.min(requestedTicks, Math.max(0L, continuousEnd - now));
        if (allowedTicks <= 0) return false;

        target.addStatusEffect(new StatusEffectInstance(
                ScytheMod.BURNS,
                allowedTicks,
                ScytheBalance.Burns.AMPLIFIER,
                false,
                true,
                true
        ));
        STATES.put(target, new State(state.continuousStart(), now + allowedTicks + ScytheBalance.Burns.IMMUNITY_TICKS));
        return true;
    }

    public static void clearAll() { STATES.clear(); }
}
