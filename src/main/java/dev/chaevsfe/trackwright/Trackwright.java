package dev.chaevsfe.trackwright;

import dev.chaevsfe.trackwright.config.ServerConfig;
import dev.chaevsfe.trackwright.network.TrackwrightNetwork;
import dev.chaevsfe.trackwright.schedule.ActorSteps;
import dev.chaevsfe.trackwright.track.PassengerResync;
import dev.chaevsfe.trackwright.undo.UndoCommand;
import dev.chaevsfe.trackwright.undo.UndoJournal;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Trackwright implements ModInitializer {
    public static final String MOD_ID = "trackwright";
    public static final Logger LOGGER = LoggerFactory.getLogger("Trackwright");

    @Override
    public void onInitialize() {
        loadHookedClasses(
            "com.simibubi.create.AllHandle",
            "net.createmod.catnip.config.ConfigBase$CValue",
            "com.simibubi.create.content.contraptions.Contraption",
            "com.simibubi.create.content.contraptions.actors.roller.RollerMovementBehaviour",
            "com.simibubi.create.content.contraptions.actors.seat.SeatMovementBehaviour",
            "com.simibubi.create.content.kinetics.base.BlockBreakingMovementBehaviour",
            "com.simibubi.create.content.trains.entity.Carriage",
            "com.simibubi.create.content.trains.entity.CarriageContraptionEntity",
            "com.simibubi.create.content.trains.entity.Navigation",
            "com.simibubi.create.content.trains.station.StationBlockEntity",
            "com.simibubi.create.content.trains.track.TrackBlock",
            "com.simibubi.create.content.trains.track.TrackBlockEntity",
            "com.simibubi.create.content.trains.track.TrackBlockItem",
            "com.simibubi.create.content.trains.track.TrackPaver",
            "com.simibubi.create.content.trains.track.TrackPlacement",
            "com.simibubi.create.content.trains.track.TrackPlacement$PlacementInfo",
            "com.simibubi.create.content.trains.track.TrackTargetingBlockItem",
            "net.minecraft.world.entity.LivingEntity"
        );
        TrackwrightNetwork.register();
        ActorSteps.register();
        ServerLifecycleEvents.SERVER_STARTING.register(server -> ServerConfig.reload());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> PassengerResync.clear());
        ServerTickEvents.END_SERVER_TICK.register(PassengerResync::tick);
        ServerTickEvents.END_SERVER_TICK.register(UndoJournal::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> UndoJournal.clearAll());
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> UndoJournal.forget(handler.player.getUUID()));
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) -> UndoJournal.clear(player));
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> UndoCommand.register(dispatcher));
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
                throw new IllegalStateException("Trackwright cannot find " + name + " in this Create build", e);
            }
        }
    }
}
