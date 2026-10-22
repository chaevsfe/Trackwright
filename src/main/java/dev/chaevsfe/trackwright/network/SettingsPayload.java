package dev.chaevsfe.trackwright.network;

import dev.chaevsfe.trackwright.Trackwright;
import dev.chaevsfe.trackwright.config.BoreMode;
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
        fields.putString(ServerConfig.BORE_THROUGH_BLOCKS, settings.boreThroughBlocks().id());
        fields.putInt(ServerConfig.BORE_HEADROOM, settings.boreHeadroom());
        fields.putBoolean(ServerConfig.BORE_LAVA_CHECK, settings.boreLavaCheck());
        fields.putDouble(ServerConfig.BORE_MAX_HARDNESS, settings.boreMaxHardness());
        fields.putBoolean(ServerConfig.REFUSE_BLOCKED_TRACK_ENDS, settings.refuseBlockedTrackEnds());
        fields.putBoolean(ServerConfig.PROTECT_TRACK_EQUIPMENT, settings.protectTrackEquipment());
        fields.putBoolean(ServerConfig.ROLLERS_SKIP_AIR_PAVING, settings.rollersSkipAirPaving());
        fields.putBoolean(ServerConfig.ROLLERS_CLEAR_POWDER_SNOW, settings.rollersClearPowderSnow());
        fields.putBoolean(ServerConfig.ROLLERS_IGNORE_FLUIDS, settings.rollersIgnoreFluids());
        fields.putBoolean(ServerConfig.CONTRAPTION_CONTROLS_SCHEDULE, settings.contraptionControlsSchedule());
        fields.putBoolean(ServerConfig.TRAIN_DRILLS_FULL_FACE, settings.trainDrillsFullFace());
        return new SettingsPayload(VERSION, fields);
    }

    public TrackwrightSettings settings() {
        return new TrackwrightSettings(
            fields.getBooleanOr(ServerConfig.STRAIGHT_THROUGH_BLOCKS, false),
            fields.getIntOr(ServerConfig.MAX_CONNECTION_LENGTH, TrackwrightSettings.MIN_CONNECTION_LENGTH),
            fields.getBooleanOr(ServerConfig.PROTECT_TRAIN_PASSENGERS, false),
            BoreMode.byId(fields.getStringOr(ServerConfig.BORE_THROUGH_BLOCKS, BoreMode.OFF.id())),
            fields.getIntOr(ServerConfig.BORE_HEADROOM, 0),
            fields.getBooleanOr(ServerConfig.BORE_LAVA_CHECK, false),
            fields.getDoubleOr(ServerConfig.BORE_MAX_HARDNESS, 0),
            fields.getBooleanOr(ServerConfig.REFUSE_BLOCKED_TRACK_ENDS, false),
            fields.getBooleanOr(ServerConfig.PROTECT_TRACK_EQUIPMENT, false),
            fields.getBooleanOr(ServerConfig.ROLLERS_SKIP_AIR_PAVING, false),
            fields.getBooleanOr(ServerConfig.ROLLERS_CLEAR_POWDER_SNOW, false),
            fields.getBooleanOr(ServerConfig.ROLLERS_IGNORE_FLUIDS, false),
            fields.getBooleanOr(ServerConfig.CONTRAPTION_CONTROLS_SCHEDULE, false),
            fields.getBooleanOr(ServerConfig.TRAIN_DRILLS_FULL_FACE, false)
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
