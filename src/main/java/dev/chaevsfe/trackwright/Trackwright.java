package dev.chaevsfe.trackwright;

import dev.chaevsfe.trackwright.config.ServerConfig;
import dev.chaevsfe.trackwright.network.TrackwrightNetwork;
import dev.chaevsfe.trackwright.track.PassengerResync;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Trackwright implements ModInitializer {
    public static final String MOD_ID = "trackwright";
    public static final Logger LOGGER = LoggerFactory.getLogger("Trackwright");

    @Override
    public void onInitialize() {
        loadHookedClasses(
            "com.zurrtum.create.AllHandle",
            "com.zurrtum.create.catnip.config.ConfigBase$CValue",
            "com.zurrtum.create.content.contraptions.actors.seat.SeatMovementBehaviour",
            "com.zurrtum.create.content.trains.entity.CarriageContraptionEntity",
            "com.zurrtum.create.content.trains.entity.Navigation",
            "com.zurrtum.create.content.trains.station.StationBlockEntity",
            "com.zurrtum.create.content.trains.track.TrackBlock",
            "com.zurrtum.create.content.trains.track.TrackBlockEntity",
            "com.zurrtum.create.content.trains.track.TrackBlockItem",
            "com.zurrtum.create.content.trains.track.TrackPlacement",
            "com.zurrtum.create.content.trains.track.TrackPlacement$PlacementInfo",
            "com.zurrtum.create.content.trains.track.TrackTargetingBlockItem",
            "net.minecraft.world.entity.LivingEntity"
        );
        TrackwrightNetwork.register();
        ServerLifecycleEvents.SERVER_STARTING.register(server -> ServerConfig.reload());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> PassengerResync.clear());
        ServerTickEvents.END_SERVER_TICK.register(PassengerResync::tick);
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            ServerConfig.reload();
            TrackwrightNetwork.sendToAll(server);
        });
    }

    public static void loadHookedClasses(String... names) {
        ClassLoader loader = Trackwright.class.getClassLoader();
        for (String name : names) {
            try {
                Class.forName(name, false, loader);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Trackwright cannot find " + name + " in this Create Fly build", e);
            }
        }
    }
}
