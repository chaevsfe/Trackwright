package dev.chaevsfe.trackwright.client;

import dev.chaevsfe.trackwright.Trackwright;
import com.zurrtum.create.AllBlockEntityTypes;
import com.zurrtum.create.client.AllBlockEntityBehaviours;
import dev.chaevsfe.trackwright.client.actors.RollerTerrainScroll;
import dev.chaevsfe.trackwright.client.config.ClientConfig;
import dev.chaevsfe.trackwright.client.guide.ActionBarHold;
import dev.chaevsfe.trackwright.client.guide.BoreShaping;
import dev.chaevsfe.trackwright.client.guide.TrackGuide;
import dev.chaevsfe.trackwright.client.guide.TrackGuideHud;
import dev.chaevsfe.trackwright.client.guide.UndoKey;
import dev.chaevsfe.trackwright.client.guide.WidestTurnLock;
import dev.chaevsfe.trackwright.client.schedule.ActorStepsClient;
import dev.chaevsfe.trackwright.client.track.FarEnd;
import dev.chaevsfe.trackwright.config.RemoteSettings;
import dev.chaevsfe.trackwright.network.SettingsPayload;
import dev.chaevsfe.trackwright.network.UndoStatePayload;
import dev.chaevsfe.trackwright.track.BoreProfiles;
import dev.chaevsfe.trackwright.track.Messages;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public final class TrackwrightClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Trackwright.loadHookedClasses(
            "com.zurrtum.create.client.content.trains.schedule.ScheduleScreen",
            "com.zurrtum.create.client.foundation.blockEntity.behaviour.ValueSettingsInputHandler",
            "com.zurrtum.create.client.foundation.blockEntity.behaviour.scrollValue.ScrollValueRenderer",
            "com.zurrtum.create.client.content.trains.track.TrackPlacementClient",
            "com.zurrtum.create.client.content.trains.track.TrackPlacementOverlay",
            "com.zurrtum.create.client.content.trains.track.TrackRenderer",
            "com.zurrtum.create.client.content.trains.track.TrackVisual",
            "com.zurrtum.create.client.foundation.gui.widget.ScrollInput",
            "com.zurrtum.create.content.trains.graph.TrackGraph",
            "net.minecraft.client.renderer.LevelRenderer",
            "net.minecraft.client.MouseHandler",
            "net.minecraft.client.renderer.ScreenEffectRenderer"
        );
        ClientConfig.load();
        AllBlockEntityBehaviours.add(AllBlockEntityTypes.MECHANICAL_ROLLER, RollerTerrainScroll::new);
        ActorStepsClient.register();
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(Trackwright.MOD_ID, "trackwright"));
        WidestTurnLock.register(category);
        UndoKey.register(category);
        BoreShaping.register(category);
        BoreProfiles.setClientSource(BoreShaping::request);
        ClientPlayNetworking.registerGlobalReceiver(SettingsPayload.TYPE, (payload, context) -> receiveSettings(payload));
        ClientPlayNetworking.registerGlobalReceiver(UndoStatePayload.TYPE, (payload, context) -> UndoKey.receive(payload));
        ClientPlayConnectionEvents.INIT.register((handler, client) -> {
            RemoteSettings.clear();
            UndoKey.reset();
            ActionBarHold.reset();
            TrackGuide.reset();
            FarEnd.reset();
            BoreShaping.reset();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            RemoteSettings.clear();
            UndoKey.reset();
            ActionBarHold.reset();
            TrackGuide.reset();
            FarEnd.reset();
            BoreShaping.reset();
        });
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay && Messages.isOwnOverlay(message)) {
                ActionBarHold.start();
            }
        });
        ClientBlockEntityEvents.BLOCK_ENTITY_LOAD.register((blockEntity, level) -> FarEnd.endLoaded(blockEntity));
        ClientBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((blockEntity, level) -> FarEnd.endUnloaded(blockEntity));
        ClientTickEvents.END_CLIENT_TICK.register(FarEnd::refresh);
        UseBlockCallback.EVENT.register(WidestTurnLock::beforeBlockUse);
        ClientTickEvents.END_CLIENT_TICK.register(WidestTurnLock::tick);
        ClientTickEvents.END_CLIENT_TICK.register(UndoKey::tick);
        ClientTickEvents.END_CLIENT_TICK.register(BoreShaping::tick);
        HudElementRegistry.attachElementBefore(
            VanillaHudElements.CHAT,
            Identifier.fromNamespaceAndPath(Trackwright.MOD_ID, "track_guide"),
            TrackGuideHud::extract
        );
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
