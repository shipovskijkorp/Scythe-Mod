package com.example.bloodyscythe.client;

public class PlagueHudState {

    private static int ticksLeft = 0;
    private static boolean active = false;

    public static void startOrUpdate(int ticks) {
        ticksLeft = Math.max(0, ticks);
        active = ticksLeft > 0;
    }

    public static void stop() {
        ticksLeft = 0;
        active = false;
    }

    public static void tick() {
        if (!active) return;

        ticksLeft--;
        if (ticksLeft <= 0) {
            stop();
        }
    }

    public static boolean isActive() {
        return active;
    }

    public static int getSecondsLeft() {
        return ticksLeft / 20;
    }
}
