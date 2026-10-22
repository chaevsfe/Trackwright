package dev.chaevsfe.trackwright.network;

import dev.chaevsfe.trackwright.Trackwright;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UndoPayload(int version, CompoundTag fields) implements CustomPacketPayload {
    public static final int VERSION = 1;
    public static final Type<UndoPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Trackwright.MOD_ID, "undo"));
    public static final StreamCodec<ByteBuf, UndoPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, UndoPayload::version,
        ByteBufCodecs.COMPOUND_TAG, UndoPayload::fields,
        UndoPayload::new
    );

    public static UndoPayload request() {
        return new UndoPayload(VERSION, new CompoundTag());
    }

    @Override
    public Type<UndoPayload> type() {
        return TYPE;
    }
}
