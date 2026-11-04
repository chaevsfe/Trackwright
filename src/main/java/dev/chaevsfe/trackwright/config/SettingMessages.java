package dev.chaevsfe.trackwright.config;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Map;

public final class SettingMessages {
    public static final String HOTKEYS_OFF = "trackwright.train_controls.off_here";
    public static final String MARKERS_OFF = "trackwright.marker.off";
    public static final String MARKERS_OPERATORS_ONLY = "trackwright.marker.operators_only";
    public static final String CLEAR_OFF = "trackwright.clear_off";
    public static final String CLEAR_CREATIVE_ONLY = "trackwright.clear_creative_only";
    public static final String CLEAR_NEEDS_SERVER = "trackwright.guide.clear_needs_server";

    private static final Map<String, String> FALLBACKS = Map.of(
        HOTKEYS_OFF, "Train control keys are turned off on this server",
        MARKERS_OFF, "Markers are off on this server",
        MARKERS_OPERATORS_ONLY, "Only operators may mark blocks on this server",
        CLEAR_OFF, "Clearing the track bed is turned off on this server",
        CLEAR_CREATIVE_ONLY, "Clearing the track bed works only in creative mode on this server",
        CLEAR_NEEDS_SERVER, "Clearing the track bed needs Trackwright 1.2 on the server"
    );

    private SettingMessages() {
    }

    public static MutableComponent component(String key) {
        String fallback = FALLBACKS.get(key);
        return fallback == null ? Component.translatable(key) : Component.translatableWithFallback(key, fallback);
    }
}
