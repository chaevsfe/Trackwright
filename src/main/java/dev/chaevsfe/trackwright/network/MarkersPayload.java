package dev.chaevsfe.trackwright.network;

import dev.chaevsfe.trackwright.Trackwright;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public record MarkersPayload(int version, CompoundTag fields) implements CustomPacketPayload {
    public static final int VERSION = 1;
    public static final String DIM = "Dim";
    public static final String CELLS = "Cells";
    public static final String TRUNCATED = "Truncated";
    public static final String CENTER = "Center";
    public static final Type<MarkersPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Trackwright.MOD_ID, "markers"));
    public static final StreamCodec<ByteBuf, MarkersPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, MarkersPayload::version,
        ByteBufCodecs.COMPOUND_TAG, MarkersPayload::fields,
        MarkersPayload::new
    );

    public record Snapshot(String dimension, long[] cells, boolean truncated, long center) {
    }

    public static MarkersPayload of(ResourceKey<Level> dimension, long[] cells, boolean truncated, ChunkPos center) {
        CompoundTag fields = new CompoundTag();
        fields.putString(DIM, dimension.identifier().toString());
        fields.putLongArray(CELLS, cells);
        fields.putBoolean(TRUNCATED, truncated);
        fields.putLong(CENTER, center.pack());
        return new MarkersPayload(VERSION, fields);
    }

    public Snapshot snapshot() {
        return new Snapshot(
            fields.getStringOr(DIM, ""),
            fields.getLongArray(CELLS).orElseGet(() -> new long[0]),
            fields.getBooleanOr(TRUNCATED, true),
            fields.getLongOr(CENTER, ChunkPos.INVALID_CHUNK_POS)
        );
    }

    @Override
    public Type<MarkersPayload> type() {
        return TYPE;
    }
}
