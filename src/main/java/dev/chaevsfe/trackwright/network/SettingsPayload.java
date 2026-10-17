package dev.chaevsfe.trackwright.network;

import dev.chaevsfe.trackwright.Trackwright;
import dev.chaevsfe.trackwright.config.ServerConfig;
import dev.chaevsfe.trackwright.config.TrackwrightSettings;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SettingsPayload(int version, CompoundTag fields) implements CustomPacketPayload {
    public static final int VERSION = 1;
    public static final Type<SettingsPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Trackwright.MOD_ID, "settings"));
    public static final StreamCodec<ByteBuf, SettingsPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, SettingsPayload::version,
        ByteBufCodecs.COMPOUND_TAG, SettingsPayload::fields,
        SettingsPayload::new
    );

    public static SettingsPayload of(TrackwrightSettings settings) {
        CompoundTag fields = new CompoundTag();
        fields.putBoolean(ServerConfig.STRAIGHT_THROUGH_BLOCKS, settings.straightThroughBlocks());
        fields.putInt(ServerConfig.MAX_CONNECTION_LENGTH, settings.maxConnectionLength());
        fields.putBoolean(ServerConfig.PROTECT_TRAIN_PASSENGERS, settings.protectTrainPassengers());
        return new SettingsPayload(VERSION, fields);
    }

    public TrackwrightSettings settings() {
        return new TrackwrightSettings(
            fields.getBooleanOr(ServerConfig.STRAIGHT_THROUGH_BLOCKS, false),
            fields.getIntOr(ServerConfig.MAX_CONNECTION_LENGTH, TrackwrightSettings.MIN_CONNECTION_LENGTH),
            fields.getBooleanOr(ServerConfig.PROTECT_TRAIN_PASSENGERS, false)
        );
    }

    public boolean sameVersion() {
        return version == VERSION;
    }

    @Override
    public Type<SettingsPayload> type() {
        return TYPE;
    }
}
