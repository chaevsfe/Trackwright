package dev.chaevsfe.trackwright.config;

import org.jspecify.annotations.Nullable;

public final class RemoteSettings {
    private static volatile @Nullable TrackwrightSettings received;

    private RemoteSettings() {
    }

    public static @Nullable TrackwrightSettings get() {
        return received;
    }

    public static void set(TrackwrightSettings settings) {
        received = settings;
    }

    public static void clear() {
        received = null;
    }
}
