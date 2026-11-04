package dev.chaevsfe.trackwright.network;

import dev.chaevsfe.trackwright.Trackwright;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

public record TrainControlPayload(int version, CompoundTag fields) implements CustomPacketPayload {
    public static final int VERSION = 1;
    public static final String TRAIN = "Train";
    public static final String CARRIAGE = "Carriage";
    public static final String POS = "Pos";
    public static final String ACTOR = "Actor";
    public static final Type<TrainControlPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Trackwright.MOD_ID, "train_control"));
    public static final StreamCodec<ByteBuf, TrainControlPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, TrainControlPayload::version,
        ByteBufCodecs.COMPOUND_TAG, TrainControlPayload::fields,
        TrainControlPayload::new
    );

    public record Press(UUID train, int carriage, BlockPos pos, String actor) {
    }

    public static TrainControlPayload of(UUID train, int carriage, BlockPos pos, String actor) {
        CompoundTag fields = new CompoundTag();
        fields.putUUID(TRAIN, train);
        fields.putInt(CARRIAGE, carriage);
        fields.putLong(POS, pos.asLong());
        fields.putString(ACTOR, actor);
        return new TrainControlPayload(VERSION, fields);
    }

    public @Nullable Press press() {
        if (version < 1) {
            return null;
        }
        if (!fields.hasUUID(TRAIN) || !fields.contains(CARRIAGE, Tag.TAG_ANY_NUMERIC) || !fields.contains(POS, Tag.TAG_ANY_NUMERIC)
            || !fields.contains(ACTOR, Tag.TAG_STRING)) {
            return null;
        }
        return new Press(fields.getUUID(TRAIN), fields.getInt(CARRIAGE), BlockPos.of(fields.getLong(POS)), fields.getString(ACTOR));
    }

    @Override
    public Type<TrainControlPayload> type() {
        return TYPE;
    }
}
