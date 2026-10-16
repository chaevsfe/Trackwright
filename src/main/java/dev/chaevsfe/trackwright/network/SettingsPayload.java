package dev.chaevsfe.trackwright.network;

import dev.chaevsfe.trackwright.Trackwright;
import dev.chaevsfe.trackwright.config.TrackwrightSettings;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SettingsPayload(boolean straightThroughBlocks, int maxConnectionLength, boolean protectTrainPassengers) implements CustomPacketPayload {
    public static final Type<SettingsPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Trackwright.MOD_ID, "settings"));
    public static final StreamCodec<ByteBuf, SettingsPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL, SettingsPayload::straightThroughBlocks,
        ByteBufCodecs.VAR_INT, SettingsPayload::maxConnectionLength,
        ByteBufCodecs.BOOL, SettingsPayload::protectTrainPassengers,
        SettingsPayload::new
    );

    public static SettingsPayload of(TrackwrightSettings settings) {
        return new SettingsPayload(settings.straightThroughBlocks(), settings.maxConnectionLength(), settings.protectTrainPassengers());
    }

    public TrackwrightSettings settings() {
        return new TrackwrightSettings(straightThroughBlocks, maxConnectionLength, protectTrainPassengers);
    }

    @Override
    public Type<SettingsPayload> type() {
        return TYPE;
    }
}
