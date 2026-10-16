package dev.chaevsfe.trackwright.track;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jspecify.annotations.Nullable;

import java.util.Map;

public final class Messages {
    public static final String OVERLAPS_STRAIGHT = "trackwright.overlaps_straight";

    private static final String KEY_PREFIX = "create.track.";
    private static final Map<String, String> FALLBACKS = Map.of(
        KEY_PREFIX + OVERLAPS_STRAIGHT, "This would overlap a straight track connection"
    );

    private Messages() {
    }

    public static @Nullable String fallback(String key) {
        return FALLBACKS.get(key);
    }

    public static MutableComponent overlay(String message) {
        String key = KEY_PREFIX + message;
        String fallback = FALLBACKS.get(key);
        return fallback != null ? Component.translatableWithFallback(key, fallback) : Component.translatable(key);
    }
}
