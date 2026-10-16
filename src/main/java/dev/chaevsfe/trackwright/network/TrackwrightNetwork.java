package dev.chaevsfe.trackwright.network;

import dev.chaevsfe.trackwright.config.ServerConfig;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class TrackwrightNetwork {
    private TrackwrightNetwork() {
    }

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(SettingsPayload.TYPE, SettingsPayload.STREAM_CODEC);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sendTo(handler.player));
    }

    public static void sendTo(ServerPlayer player) {
        if (ServerPlayNetworking.canSend(player, SettingsPayload.TYPE)) {
            ServerPlayNetworking.send(player, SettingsPayload.of(ServerConfig.get()));
        }
    }

    public static void sendToAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendTo(player);
        }
    }
}
