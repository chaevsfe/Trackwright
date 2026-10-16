package dev.chaevsfe.trackwright.config;

import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public final class Sides {
    private Sides() {
    }

    public static @Nullable TrackwrightSettings forLevel(Level level) {
        return level.isClientSide() ? RemoteSettings.get() : ServerConfig.get();
    }
}
