package dev.chaevsfe.trackwright.network;

import dev.chaevsfe.trackwright.Trackwright;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record UndoStatePayload(int version, int placements, int ticksLeft) implements CustomPacketPayload {
    public static final int VERSION = 1;
    public static final Type<UndoStatePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Trackwright.MOD_ID, "undo_state"));
    public static final StreamCodec<ByteBuf, UndoStatePayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, UndoStatePayload::version,
        ByteBufCodecs.VAR_INT, UndoStatePayload::placements,
        ByteBufCodecs.VAR_INT, UndoStatePayload::ticksLeft,
        UndoStatePayload::new
    );

    @Override
    public Type<UndoStatePayload> type() {
        return TYPE;
    }
}
