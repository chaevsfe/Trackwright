package dev.chaevsfe.trackwright.network;

import dev.chaevsfe.trackwright.TagReads;
import dev.chaevsfe.trackwright.Trackwright;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public record MarkersPayload(int version, CompoundTag fields) implements CustomPacketPayload {
    public static final int VERSION = 1;
    public static final String DIM = "Dim";
    public static final String CELLS = "Cells";
    public static final String TRUNCATED = "Truncated";
    public static final String CENTER = "Center";
    public static final Type<MarkersPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Trackwright.MOD_ID, "markers"));
    public static final StreamCodec<ByteBuf, MarkersPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, MarkersPayload::version,
        ByteBufCodecs.COMPOUND_TAG, MarkersPayload::fields,
        MarkersPayload::new
    );

    public record Snapshot(String dimension, long[] cells, boolean truncated, long center) {
    }

    public static MarkersPayload of(ResourceKey<Level> dimension, long[] cells, boolean truncated, ChunkPos center) {
        CompoundTag fields = new CompoundTag();
        fields.putString(DIM, dimension.location().toString());
        fields.putLongArray(CELLS, cells);
        fields.putBoolean(TRUNCATED, truncated);
        fields.putLong(CENTER, center.toLong());
        return new MarkersPayload(VERSION, fields);
    }

    public Snapshot snapshot() {
        return new Snapshot(
            TagReads.stringOr(fields, DIM, ""),
            fields.contains(CELLS, Tag.TAG_LONG_ARRAY) ? fields.getLongArray(CELLS) : new long[0],
            TagReads.booleanOr(fields, TRUNCATED, true),
            fields.contains(CENTER, Tag.TAG_ANY_NUMERIC) ? fields.getLong(CENTER) : ChunkPos.INVALID_CHUNK_POS
        );
    }

    @Override
    public Type<MarkersPayload> type() {
        return TYPE;
    }
}
