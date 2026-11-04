package dev.chaevsfe.trackwright.client.guide;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class RadiusLock {
    private RadiusLock() {
    }

    public static boolean beforeUseItem(Minecraft mc, LocalPlayer player) {
        return false;
    }

    public static boolean onScroll(int wheel) {
        return false;
    }
}
