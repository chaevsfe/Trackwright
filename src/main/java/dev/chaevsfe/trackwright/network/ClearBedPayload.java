package dev.chaevsfe.trackwright.network;

import dev.chaevsfe.trackwright.Trackwright;
import dev.chaevsfe.trackwright.config.BoreProfile;
import dev.chaevsfe.trackwright.config.ServerConfig;
import dev.chaevsfe.trackwright.track.BoreProfiles;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
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
    public static final Type<ClearBedPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Trackwright.MOD_ID, "clear_bed"));
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
        if (!fields.contains(AIM_X) || !fields.contains(AIM_Y) || !fields.contains(AIM_Z)) {
            return null;
        }
        double x = fields.getDoubleOr(AIM_X, 0);
        double y = fields.getDoubleOr(AIM_Y, 0);
        double z = fields.getDoubleOr(AIM_Z, 0);
        return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z) ? new Vec3(x, y, z) : null;
    }

    public @Nullable Long plan() {
        return fields.contains(PLAN) ? fields.getLongOr(PLAN, 0) : null;
    }

    public int count() {
        return fields.getIntOr(COUNT, -1);
    }

    public BoreProfiles.Request request() {
        CompoundTag profile = fields.getCompound(PlacementPayload.BORE_PROFILE).orElse(null);
        if (profile == null) {
            return BoreProfiles.Request.NONE;
        }
        return new BoreProfiles.Request(read(profile, ServerConfig.LEFT), read(profile, ServerConfig.RIGHT), read(profile, ServerConfig.UP));
    }

    private @Nullable BlockPos position(String key) {
        return fields.contains(key) ? BlockPos.of(fields.getLongOr(key, 0)) : null;
    }

    private static @Nullable Integer read(CompoundTag tag, String key) {
        return tag.contains(key) ? tag.getIntOr(key, 0) : null;
    }

    @Override
    public Type<ClearBedPayload> type() {
        return TYPE;
    }
}
