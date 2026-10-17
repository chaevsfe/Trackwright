package dev.chaevsfe.trackwright.client.guide;

import net.minecraft.util.Util;

public final class ActionBarHold {
    private static final long HOLD_MILLIS = 3000;

    private static long until;

    private ActionBarHold() {
    }

    public static void start() {
        until = Util.getMillis() + HOLD_MILLIS;
    }

    public static boolean active() {
        return Util.getMillis() < until;
    }

    public static void reset() {
        until = 0;
    }
}
