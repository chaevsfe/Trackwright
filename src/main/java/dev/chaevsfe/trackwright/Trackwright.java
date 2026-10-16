package dev.chaevsfe.trackwright;

import dev.chaevsfe.trackwright.config.ServerConfig;
import dev.chaevsfe.trackwright.network.TrackwrightNetwork;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Trackwright implements ModInitializer {
    public static final String MOD_ID = "trackwright";
    public static final Logger LOGGER = LoggerFactory.getLogger("Trackwright");

    @Override
    public void onInitialize() {
        TrackwrightNetwork.register();
        ServerLifecycleEvents.SERVER_STARTING.register(server -> ServerConfig.reload());
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            ServerConfig.reload();
            TrackwrightNetwork.sendToAll(server);
        });
    }
}
