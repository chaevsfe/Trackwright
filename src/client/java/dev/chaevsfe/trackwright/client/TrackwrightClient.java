package dev.chaevsfe.trackwright.client;

import dev.chaevsfe.trackwright.config.RemoteSettings;
import dev.chaevsfe.trackwright.network.SettingsPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class TrackwrightClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(SettingsPayload.TYPE, (payload, context) -> RemoteSettings.set(payload.settings()));
        ClientPlayConnectionEvents.INIT.register((handler, client) -> RemoteSettings.clear());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> RemoteSettings.clear());
    }
}
