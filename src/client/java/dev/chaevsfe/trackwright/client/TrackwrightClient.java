package dev.chaevsfe.trackwright.client;

import dev.chaevsfe.trackwright.Trackwright;
import dev.chaevsfe.trackwright.client.config.ClientConfig;
import dev.chaevsfe.trackwright.client.guide.ActionBarHold;
import dev.chaevsfe.trackwright.client.guide.TrackGuide;
import dev.chaevsfe.trackwright.client.guide.TrackGuideHud;
import dev.chaevsfe.trackwright.client.guide.UndoKey;
import dev.chaevsfe.trackwright.client.guide.WidestTurnLock;
import dev.chaevsfe.trackwright.client.track.FarEnd;
import dev.chaevsfe.trackwright.config.RemoteSettings;
import dev.chaevsfe.trackwright.network.SettingsPayload;
import dev.chaevsfe.trackwright.network.TrackwrightNetwork;
import dev.chaevsfe.trackwright.track.Messages;
import dev.chaevsfe.trackwright.track.PassengerProtection;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderBlockScreenEffectEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@Mod(value = Trackwright.MOD_ID, dist = Dist.CLIENT)
public final class TrackwrightClient {
    public static final String KEY_CATEGORY = "key.category.trackwright.trackwright";
    public static final ResourceLocation GUIDE_LAYER = ResourceLocation.fromNamespaceAndPath(Trackwright.MOD_ID, "track_guide");

    public TrackwrightClient(IEventBus modBus) {
        ClientConfig.load();
        TrackwrightNetwork.receiveOnClient(TrackwrightClient::receiveSettings, UndoKey::receive);
        modBus.addListener(FMLClientSetupEvent.class, event -> event.enqueueWork(TrackwrightClient::loadHookedClasses));
        modBus.addListener(RegisterKeyMappingsEvent.class, event -> {
            event.register(WidestTurnLock.create());
            event.register(UndoKey.create());
        });
        modBus.addListener(RegisterGuiLayersEvent.class, event -> event.registerBelow(VanillaGuiLayers.CHAT, GUIDE_LAYER, TrackGuideHud::render));
        IEventBus game = NeoForge.EVENT_BUS;
        game.addListener(ClientPlayerNetworkEvent.LoggingIn.class, event -> reset());
        game.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> reset());
        game.addListener(EventPriority.LOWEST, false, ClientChatReceivedEvent.System.class, event -> {
            if (event.isOverlay() && Messages.isOwnOverlay(event.getMessage())) {
                ActionBarHold.start();
            }
        });
        game.addListener(ClientTickEvent.Post.class, event -> {
            Minecraft mc = Minecraft.getInstance();
            FarEnd.refresh(mc);
            WidestTurnLock.tick(mc);
            UndoKey.tick(mc);
        });
        game.addListener(EventPriority.LOWEST, false, PlayerInteractEvent.RightClickBlock.class, WidestTurnLock::beforeBlockUse);
        game.addListener(RenderBlockScreenEffectEvent.class, event -> {
            if (event.getOverlayType() == RenderBlockScreenEffectEvent.OverlayType.BLOCK && PassengerProtection.isProtectedOnClient(event.getPlayer())) {
                event.setCanceled(true);
            }
        });
    }

    private static void loadHookedClasses() {
        Trackwright.loadHookedClasses(
            "com.simibubi.create.content.trains.schedule.ScheduleScreen",
            "com.simibubi.create.content.trains.track.TrackPlacement",
            "com.simibubi.create.content.trains.track.TrackPlacementOverlay",
            "com.simibubi.create.content.trains.track.TrackRenderer",
            "com.simibubi.create.content.trains.track.TrackVisual",
            "dev.engine_room.flywheel.lib.visual.AbstractVisual",
            "com.simibubi.create.foundation.gui.widget.ScrollInput",
            "com.simibubi.create.content.trains.graph.TrackGraph",
            "net.minecraft.client.renderer.LevelRenderer",
            "net.minecraft.client.multiplayer.ClientChunkCache",
            "net.minecraft.world.level.chunk.LevelChunk"
        );
    }

    private static void reset() {
        RemoteSettings.clear();
        UndoKey.reset();
        ActionBarHold.reset();
        TrackGuide.reset();
        FarEnd.reset();
    }

    private static void receiveSettings(SettingsPayload payload) {
        if (!payload.sameVersion()) {
            Trackwright.LOGGER.info("This server's Trackwright sends settings format {} and this client reads format {}; only the settings both know are used",
                payload.version(), SettingsPayload.VERSION);
        }
        RemoteSettings.set(payload.settings());
        TrackGuide.forgetPlan();
    }
}
