package dev.chaevsfe.trackwright.config;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.entity.player.Player;
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

    public boolean allows(CommandSourceStack source) {
        return allows(source.hasPermission(Commands.LEVEL_GAMEMASTERS));
    }

    public boolean allows(Player player) {
        return allows(player.hasPermissions(Commands.LEVEL_GAMEMASTERS));
    }

    private boolean allows(boolean operator) {
        return switch (this) {
            case OFF -> false;
            case OPERATORS -> operator;
            case EVERYONE -> true;
        };
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
