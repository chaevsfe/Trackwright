package dev.chaevsfe.trackwright.network;

import dev.chaevsfe.trackwright.config.ServerConfig;
import dev.chaevsfe.trackwright.track.BoreProfiles;
import dev.chaevsfe.trackwright.undo.TrackUndo;
import dev.chaevsfe.trackwright.undo.UndoJournal;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.function.Consumer;

public final class TrackwrightNetwork {
    public static final String PROTOCOL = "1";

    private static volatile Consumer<SettingsPayload> settingsReceiver = payload -> {
    };
    private static volatile Consumer<UndoStatePayload> undoStateReceiver = payload -> {
    };

    private TrackwrightNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL).optional();
        registrar.playToClient(SettingsPayload.TYPE, SettingsPayload.STREAM_CODEC, (payload, context) -> settingsReceiver.accept(payload));
        registrar.playToClient(UndoStatePayload.TYPE, UndoStatePayload.STREAM_CODEC, (payload, context) -> undoStateReceiver.accept(payload));
        registrar.playToServer(PlacementPayload.TYPE, PlacementPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                BoreProfiles.receive(player, payload.request());
            }
        });
        registrar.playToServer(UndoPayload.TYPE, UndoPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player && UndoJournal.acceptRequest(player)) {
                TrackUndo.undoAndTell(player);
            }
        });
    }

    public static void receiveOnClient(Consumer<SettingsPayload> settings, Consumer<UndoStatePayload> undoState) {
        settingsReceiver = settings;
        undoStateReceiver = undoState;
    }

    public static boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type) {
        return !(player instanceof FakePlayer) && player.connection != null && player.connection.getConnection().channel() != null
            && player.connection.hasChannel(type);
    }

    public static boolean hasTrackwright(ServerPlayer player) {
        return canSend(player, SettingsPayload.TYPE);
    }

    public static void sendTo(ServerPlayer player) {
        if (hasTrackwright(player)) {
            PacketDistributor.sendToPlayer(player, SettingsPayload.of(ServerConfig.get()));
        }
    }

    public static void sendUndoState(ServerPlayer player, int placements, int ticksLeft, boolean result) {
        if (canSend(player, UndoStatePayload.TYPE)) {
            PacketDistributor.sendToPlayer(player, UndoStatePayload.of(placements, ticksLeft, result));
        }
    }

    public static void sendToAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendTo(player);
        }
    }
}
