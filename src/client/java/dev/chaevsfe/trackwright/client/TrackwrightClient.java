package dev.chaevsfe.trackwright.client;

import dev.chaevsfe.trackwright.Trackwright;
import dev.chaevsfe.trackwright.client.guide.TrackGuide;
import dev.chaevsfe.trackwright.client.guide.TrackGuideHud;
import dev.chaevsfe.trackwright.config.RemoteSettings;
import dev.chaevsfe.trackwright.network.SettingsPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;

public final class TrackwrightClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(SettingsPayload.TYPE, (payload, context) -> RemoteSettings.set(payload.settings()));
        ClientPlayConnectionEvents.INIT.register((handler, client) -> {
            RemoteSettings.clear();
            TrackGuide.reset();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            RemoteSettings.clear();
            TrackGuide.reset();
        });
        HudElementRegistry.attachElementBefore(
            VanillaHudElements.CHAT,
            Identifier.fromNamespaceAndPath(Trackwright.MOD_ID, "track_guide"),
            TrackGuideHud::extract
        );
    }
}
