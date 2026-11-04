package dev.chaevsfe.trackwright.network;

import dev.chaevsfe.trackwright.TagReads;
import dev.chaevsfe.trackwright.Trackwright;
import dev.chaevsfe.trackwright.config.BoreProfile;
import dev.chaevsfe.trackwright.config.ServerConfig;
import dev.chaevsfe.trackwright.track.BoreProfiles;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public record ClearBedPayload(int version, CompoundTag fields) implements CustomPacketPayload {
    public static final int VERSION = 1;
    public static final String TRACK = "track";
    public static final String TARGET = "target";
    public static final String AIM_X = "aimX";
    public static final String AIM_Y = "aimY";
    public static final String AIM_Z = "aimZ";
    public static final String PLAN = "plan";
    public static final String COUNT = "count";
    public static final Type<ClearBedPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Trackwright.MOD_ID, "clear_bed"));
    public static final StreamCodec<ByteBuf, ClearBedPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, ClearBedPayload::version,
        ByteBufCodecs.COMPOUND_TAG, ClearBedPayload::fields,
        ClearBedPayload::new
    );

    public static ClearBedPayload of(BlockPos track, BlockPos target, Vec3 aim, @Nullable BoreProfile shaped, @Nullable Long plan, int count) {
        CompoundTag fields = new CompoundTag();
        fields.putLong(TRACK, track.asLong());
        fields.putLong(TARGET, target.asLong());
        fields.putDouble(AIM_X, aim.x);
        fields.putDouble(AIM_Y, aim.y);
        fields.putDouble(AIM_Z, aim.z);
        if (shaped != null) {
            fields.put(PlacementPayload.BORE_PROFILE, SettingsPayload.profileTag(shaped));
        }
        if (plan != null) {
            fields.putLong(PLAN, plan);
            fields.putInt(COUNT, count);
        }
        return new ClearBedPayload(VERSION, fields);
    }

    public @Nullable BlockPos track() {
        return position(TRACK);
    }

    public @Nullable BlockPos target() {
        return position(TARGET);
    }

    public @Nullable Vec3 aim() {
        if (!fields.contains(AIM_X, Tag.TAG_ANY_NUMERIC) || !fields.contains(AIM_Y, Tag.TAG_ANY_NUMERIC) || !fields.contains(AIM_Z, Tag.TAG_ANY_NUMERIC)) {
            return null;
        }
        double x = TagReads.doubleOr(fields, AIM_X, 0);
        double y = TagReads.doubleOr(fields, AIM_Y, 0);
        double z = TagReads.doubleOr(fields, AIM_Z, 0);
        return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z) ? new Vec3(x, y, z) : null;
    }

    public @Nullable Long plan() {
        return fields.contains(PLAN, Tag.TAG_ANY_NUMERIC) ? fields.getLong(PLAN) : null;
    }

    public int count() {
        return TagReads.intOr(fields, COUNT, -1);
    }

    public BoreProfiles.Request request() {
        if (!fields.contains(PlacementPayload.BORE_PROFILE, Tag.TAG_COMPOUND)) {
            return BoreProfiles.Request.NONE;
        }
        CompoundTag profile = fields.getCompound(PlacementPayload.BORE_PROFILE);
        return new BoreProfiles.Request(read(profile, ServerConfig.LEFT), read(profile, ServerConfig.RIGHT), read(profile, ServerConfig.UP));
    }

    private @Nullable BlockPos position(String key) {
        return fields.contains(key, Tag.TAG_ANY_NUMERIC) ? BlockPos.of(fields.getLong(key)) : null;
    }

    private static @Nullable Integer read(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getInt(key) : null;
    }

    @Override
    public Type<ClearBedPayload> type() {
        return TYPE;
    }
}
