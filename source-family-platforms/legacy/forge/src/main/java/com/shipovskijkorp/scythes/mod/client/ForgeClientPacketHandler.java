package com.shipovskijkorp.scythes.mod.client;

import com.shipovskijkorp.scythes.mod.platform.HudTransport;

public final class ForgeClientPacketHandler {
    private ForgeClientPacketHandler() {}

    public static void handleHud(int timerId, boolean active, int ticksLeft) {
        HudTransport.Timer[] timers = HudTransport.Timer.values();
        if (timerId < 0 || timerId >= timers.length) return;

        switch (timers[timerId]) {
            case BLOOD_HARVEST -> {
                if (active) BloodHarvestHudState.startOrUpdate(ticksLeft);
                else BloodHarvestHudState.stop();
            }
            case TOXIC_AURA -> {
                if (active) ToxicAuraHudState.startOrUpdate(ticksLeft);
                else ToxicAuraHudState.stop();
            }
            case WITHERING_AURA -> {
                if (active) WitheringAuraHudState.startOrUpdate(ticksLeft);
                else WitheringAuraHudState.stop();
            }
        }
    }
}
