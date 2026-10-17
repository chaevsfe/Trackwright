package dev.chaevsfe.trackwright.config;

import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

public enum BoreMode {
    OFF("off"),
    SURVIVAL("survival"),
    CREATIVE_ONLY("creative-only");

    private final String id;

    BoreMode(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public boolean allows(Player player) {
        return switch (this) {
            case OFF -> false;
            case SURVIVAL -> true;
            case CREATIVE_ONLY -> player.isCreative();
        };
    }

    public static @Nullable BoreMode byId(@Nullable String id) {
        for (BoreMode mode : values()) {
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
