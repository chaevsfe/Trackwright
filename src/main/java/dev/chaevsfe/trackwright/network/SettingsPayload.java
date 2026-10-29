package dev.chaevsfe.trackwright.network;

import dev.chaevsfe.trackwright.TagReads;
import dev.chaevsfe.trackwright.Trackwright;
import dev.chaevsfe.trackwright.config.BoreMode;
import dev.chaevsfe.trackwright.config.ServerConfig;
import dev.chaevsfe.trackwright.config.TrackwrightSettings;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SettingsPayload(int version, CompoundTag fields) implements CustomPacketPayload {
    public static final int VERSION = 2;
    public static final Type<SettingsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Trackwright.MOD_ID, "settings"));
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
        fields.putBoolean(ServerConfig.ROLLERS_NEED_GROUND, settings.rollersNeedGround());
        fields.putBoolean(ServerConfig.ROLLER_RESHAPE, settings.rollerReshape());
        return new SettingsPayload(VERSION, fields);
    }

    public TrackwrightSettings settings() {
        return new TrackwrightSettings(
            TagReads.booleanOr(fields, ServerConfig.STRAIGHT_THROUGH_BLOCKS, false),
            TagReads.intOr(fields, ServerConfig.MAX_CONNECTION_LENGTH, TrackwrightSettings.MIN_CONNECTION_LENGTH),
            TagReads.booleanOr(fields, ServerConfig.PROTECT_TRAIN_PASSENGERS, false),
            BoreMode.byId(TagReads.stringOr(fields, ServerConfig.BORE_THROUGH_BLOCKS, BoreMode.OFF.id())),
            TagReads.intOr(fields, ServerConfig.BORE_HEADROOM, 0),
            TagReads.booleanOr(fields, ServerConfig.BORE_LAVA_CHECK, false),
            TagReads.doubleOr(fields, ServerConfig.BORE_MAX_HARDNESS, 0),
            TagReads.booleanOr(fields, ServerConfig.REFUSE_BLOCKED_TRACK_ENDS, false),
            TagReads.booleanOr(fields, ServerConfig.PROTECT_TRACK_EQUIPMENT, false),
            TagReads.booleanOr(fields, ServerConfig.ROLLERS_SKIP_AIR_PAVING, false),
            TagReads.booleanOr(fields, ServerConfig.ROLLERS_CLEAR_POWDER_SNOW, false),
            TagReads.booleanOr(fields, ServerConfig.ROLLERS_IGNORE_FLUIDS, false),
            TagReads.booleanOr(fields, ServerConfig.CONTRAPTION_CONTROLS_SCHEDULE, false),
            TagReads.booleanOr(fields, ServerConfig.TRAIN_DRILLS_FULL_FACE, false),
            TagReads.booleanOr(fields, ServerConfig.ROLLERS_NEED_GROUND, false),
            TagReads.booleanOr(fields, ServerConfig.ROLLER_RESHAPE, false)
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
