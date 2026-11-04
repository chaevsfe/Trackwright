package dev.chaevsfe.trackwright.config;

import org.jspecify.annotations.Nullable;

public enum MarkerMode {
    OFF("off"),
    OPERATORS("operators"),
    EVERYONE("everyone");

    private final String id;

    MarkerMode(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public boolean enabled() {
        return this != OFF;
    }

    public static @Nullable MarkerMode byId(@Nullable String id) {
        for (MarkerMode mode : values()) {
            if (mode.id.equals(id)) {
                return mode;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return id;
    }
}
