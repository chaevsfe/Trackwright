package dev.chaevsfe.trackwright.config;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public final class Sides {
    private static final boolean DEDICATED = FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER;
    private static volatile @Nullable MinecraftServer server;

    private Sides() {
    }

    public static void serverStarting(MinecraftServer starting) {
        server = starting;
    }

    public static void serverStopped(MinecraftServer stopped) {
        if (server == stopped) {
            server = null;
        }
    }

    public static boolean onServerThread() {
        if (DEDICATED) {
            return true;
        }
        MinecraftServer running = server;
        return running != null && running.isSameThread();
    }

    public static @Nullable TrackwrightSettings forLevel(Level level) {
        return level.isClientSide() ? RemoteSettings.get() : ServerConfig.get();
    }

    public static @Nullable TrackwrightSettings forCurrentThread() {
        return onServerThread() ? ServerConfig.get() : RemoteSettings.get();
    }
}
