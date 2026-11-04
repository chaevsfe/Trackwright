package dev.chaevsfe.trackwright.network;

import dev.chaevsfe.trackwright.Trackwright;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

public record TrainControlPayload(int version, CompoundTag fields) implements CustomPacketPayload {
    public static final int VERSION = 1;
    public static final String TRAIN = "Train";
    public static final String CARRIAGE = "Carriage";
    public static final String POS = "Pos";
    public static final String ACTOR = "Actor";
    public static final Type<TrainControlPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Trackwright.MOD_ID, "train_control"));
    public static final StreamCodec<ByteBuf, TrainControlPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, TrainControlPayload::version,
        ByteBufCodecs.COMPOUND_TAG, TrainControlPayload::fields,
        TrainControlPayload::new
    );

    public record Press(UUID train, int carriage, BlockPos pos, String actor) {
    }

    public static TrainControlPayload of(UUID train, int carriage, BlockPos pos, String actor) {
        CompoundTag fields = new CompoundTag();
        fields.store(TRAIN, UUIDUtil.CODEC, train);
        fields.putInt(CARRIAGE, carriage);
        fields.putLong(POS, pos.asLong());
        fields.putString(ACTOR, actor);
        return new TrainControlPayload(VERSION, fields);
    }

    public @Nullable Press press() {
        if (version < 1) {
            return null;
        }
        UUID train = fields.read(TRAIN, UUIDUtil.CODEC).orElse(null);
        Integer carriage = fields.getInt(CARRIAGE).orElse(null);
        Long pos = fields.getLong(POS).orElse(null);
        String actor = fields.getString(ACTOR).orElse(null);
        if (train == null || carriage == null || pos == null || actor == null) {
            return null;
        }
        return new Press(train, carriage, BlockPos.of(pos), actor);
    }

    @Override
    public Type<TrainControlPayload> type() {
        return TYPE;
    }
}
