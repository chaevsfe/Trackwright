package dev.chaevsfe.trackwright.network;

import dev.chaevsfe.trackwright.Trackwright;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record UndoStatePayload(int version, CompoundTag fields) implements CustomPacketPayload {
    public static final int VERSION = 1;
    private static final String PLACEMENTS = "placements";
    private static final String TICKS_LEFT = "ticksLeft";
    public static final Type<UndoStatePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Trackwright.MOD_ID, "undo_state"));
    public static final StreamCodec<ByteBuf, UndoStatePayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, UndoStatePayload::version,
        ByteBufCodecs.COMPOUND_TAG, UndoStatePayload::fields,
        UndoStatePayload::new
    );

    public static UndoStatePayload of(int placements, int ticksLeft) {
        CompoundTag fields = new CompoundTag();
        fields.putInt(PLACEMENTS, placements);
        fields.putInt(TICKS_LEFT, ticksLeft);
        return new UndoStatePayload(VERSION, fields);
    }

    public int placements() {
        return fields.getIntOr(PLACEMENTS, 0);
    }

    public int ticksLeft() {
        return fields.getIntOr(TICKS_LEFT, 0);
    }

    @Override
    public Type<UndoStatePayload> type() {
        return TYPE;
    }
}
