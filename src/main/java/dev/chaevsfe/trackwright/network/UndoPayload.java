package dev.chaevsfe.trackwright.network;

import dev.chaevsfe.trackwright.Trackwright;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record UndoPayload(int version) implements CustomPacketPayload {
    public static final int VERSION = 1;
    public static final Type<UndoPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Trackwright.MOD_ID, "undo"));
    public static final StreamCodec<ByteBuf, UndoPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, UndoPayload::version,
        UndoPayload::new
    );

    @Override
    public Type<UndoPayload> type() {
        return TYPE;
    }
}
