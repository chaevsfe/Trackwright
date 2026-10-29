package dev.chaevsfe.trackwright;

import dev.chaevsfe.trackwright.config.ServerConfig;
import dev.chaevsfe.trackwright.network.TrackwrightNetwork;
import dev.chaevsfe.trackwright.schedule.ActorSteps;
import dev.chaevsfe.trackwright.track.PassengerResync;
import dev.chaevsfe.trackwright.undo.UndoCommand;
import dev.chaevsfe.trackwright.undo.UndoJournal;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(Trackwright.MOD_ID)
public final class Trackwright {
    public static final String MOD_ID = "trackwright";
    public static final Logger LOGGER = LoggerFactory.getLogger("Trackwright");

    public Trackwright(IEventBus modBus) {
        modBus.addListener(FMLCommonSetupEvent.class, event -> event.enqueueWork(Trackwright::setUp));
        modBus.addListener(RegisterPayloadHandlersEvent.class, TrackwrightNetwork::register);
        IEventBus game = NeoForge.EVENT_BUS;
        game.addListener(ServerAboutToStartEvent.class, event -> ServerConfig.reload());
        game.addListener(ServerStoppedEvent.class, event -> {
            PassengerResync.clear();
            UndoJournal.clearAll();
        });
        game.addListener(ServerTickEvent.Post.class, event -> {
            PassengerResync.tick(event.getServer());
            UndoJournal.tick(event.getServer());
        });
        game.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                TrackwrightNetwork.sendTo(player);
            }
        });
        game.addListener(EventPriority.LOWEST, true, BlockEvent.EntityPlaceEvent.class, UndoJournal::placeEventPosted);
        game.addListener(PlayerEvent.PlayerLoggedOutEvent.class, event -> UndoJournal.forget(event.getEntity().getUUID()));
        game.addListener(PlayerEvent.PlayerChangedDimensionEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                UndoJournal.clear(player);
            }
        });
        game.addListener(PlayerEvent.PlayerRespawnEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                UndoJournal.respawned(player);
            }
        });
        game.addListener(RegisterCommandsEvent.class, event -> UndoCommand.register(event.getDispatcher()));
        game.addListener(AddReloadListenerEvent.class, event -> reloadSettings());
    }

    private static void setUp() {
        loadHookedClasses(
            "com.simibubi.create.content.contraptions.Contraption",
            "com.simibubi.create.content.contraptions.actors.roller.RollerBlockEntity",
            "com.simibubi.create.content.contraptions.actors.roller.RollerMovementBehaviour",
            "com.simibubi.create.content.contraptions.actors.seat.SeatMovementBehaviour",
            "com.simibubi.create.content.kinetics.base.BlockBreakingMovementBehaviour",
            "com.simibubi.create.content.trains.entity.Carriage",
            "com.simibubi.create.content.trains.entity.CarriageContraptionEntity",
            "com.simibubi.create.content.trains.entity.Navigation",
            "com.simibubi.create.content.trains.graph.TrackGraph",
            "com.simibubi.create.content.trains.schedule.ScheduleEditPacket",
            "com.simibubi.create.content.trains.station.StationBlockEntity",
            "com.simibubi.create.content.trains.track.CurvedTrackDestroyPacket",
            "com.simibubi.create.content.trains.track.CurvedTrackSelectionPacket",
            "com.simibubi.create.content.trains.track.TrackBlock",
            "com.simibubi.create.content.trains.track.TrackBlockEntity",
            "com.simibubi.create.content.trains.track.TrackBlockItem",
            "com.simibubi.create.content.trains.track.TrackPaver",
            "com.simibubi.create.content.trains.track.TrackPlacement",
            "com.simibubi.create.content.trains.track.TrackPlacement$PlacementInfo",
            "com.simibubi.create.content.trains.track.TrackTargetingBlockItem",
            "com.simibubi.create.foundation.networking.BlockEntityConfigurationPacket",
            "net.minecraft.server.network.ServerGamePacketListenerImpl",
            "net.minecraft.world.entity.LivingEntity"
        );
        ActorSteps.register();
    }

    private static void reloadSettings() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        server.execute(() -> {
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
