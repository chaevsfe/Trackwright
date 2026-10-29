package dev.chaevsfe.trackwright.network;

import dev.chaevsfe.trackwright.Trackwright;
import dev.chaevsfe.trackwright.config.BoreProfile;
import dev.chaevsfe.trackwright.config.ServerConfig;
import dev.chaevsfe.trackwright.track.BoreProfiles;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jspecify.annotations.Nullable;

public record PlacementPayload(int version, CompoundTag fields) implements CustomPacketPayload {
    public static final int VERSION = 1;
    public static final String BORE_PROFILE = "boreProfile";
    public static final Type<PlacementPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Trackwright.MOD_ID, "placement"));
    public static final StreamCodec<ByteBuf, PlacementPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, PlacementPayload::version,
        ByteBufCodecs.COMPOUND_TAG, PlacementPayload::fields,
        PlacementPayload::new
    );

    public static PlacementPayload of(@Nullable BoreProfile shaped) {
        CompoundTag fields = new CompoundTag();
        if (shaped != null) {
            fields.put(BORE_PROFILE, SettingsPayload.profileTag(shaped));
        }
        return new PlacementPayload(VERSION, fields);
    }

    public BoreProfiles.Request request() {
        if (!fields.contains(BORE_PROFILE, Tag.TAG_COMPOUND)) {
            return BoreProfiles.Request.NONE;
        }
        CompoundTag profile = fields.getCompound(BORE_PROFILE);
        return new BoreProfiles.Request(read(profile, ServerConfig.LEFT), read(profile, ServerConfig.RIGHT), read(profile, ServerConfig.UP));
    }

    private static @Nullable Integer read(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getInt(key) : null;
    }

    @Override
    public Type<PlacementPayload> type() {
        return TYPE;
    }
}
