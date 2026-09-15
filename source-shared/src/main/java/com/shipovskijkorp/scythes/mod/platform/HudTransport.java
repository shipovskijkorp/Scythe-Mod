package com.shipovskijkorp.scythes.mod.platform;

/** Loader-independent transport contract; start/stop packets keep their original IDs. */
public interface HudTransport<P> {
    enum Timer { BLOOD_HARVEST, TOXIC_AURA, WITHERING_AURA }
    void start(P player, Timer timer, int ticksLeft);
    void stop(P player, Timer timer);
}
