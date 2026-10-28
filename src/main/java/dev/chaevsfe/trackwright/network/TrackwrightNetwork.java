package dev.chaevsfe.trackwright.network;

import dev.chaevsfe.trackwright.config.ServerConfig;
import dev.chaevsfe.trackwright.track.BoreProfiles;
import dev.chaevsfe.trackwright.undo.TrackUndo;
import dev.chaevsfe.trackwright.undo.UndoJournal;
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
        PayloadTypeRegistry.clientboundPlay().register(UndoStatePayload.TYPE, UndoStatePayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(UndoPayload.TYPE, UndoPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(PlacementPayload.TYPE, PlacementPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(PlacementPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            BoreProfiles.Request request = payload.request();
            context.server().execute(() -> BoreProfiles.receive(player, request));
        });
        ServerPlayNetworking.registerGlobalReceiver(UndoPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                if (UndoJournal.acceptRequest(player)) {
                    TrackUndo.undoAndTell(player);
                }
            });
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sendTo(handler.player));
    }

    public static boolean hasTrackwright(ServerPlayer player) {
        return ServerPlayNetworking.canSend(player, SettingsPayload.TYPE);
    }

    public static void sendTo(ServerPlayer player) {
        if (hasTrackwright(player)) {
            ServerPlayNetworking.send(player, SettingsPayload.of(ServerConfig.get()));
        }
    }

    public static void sendUndoState(ServerPlayer player, int placements, int ticksLeft, boolean result) {
        if (ServerPlayNetworking.canSend(player, UndoStatePayload.TYPE)) {
            ServerPlayNetworking.send(player, UndoStatePayload.of(placements, ticksLeft, result));
        }
    }

    public static void sendToAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendTo(player);
        }
    }
}
