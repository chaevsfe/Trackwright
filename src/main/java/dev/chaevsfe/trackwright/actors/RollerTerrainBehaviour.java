package dev.chaevsfe.trackwright.actors;

import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.foundation.blockEntity.behaviour.BehaviourType;
import com.zurrtum.create.foundation.blockEntity.behaviour.scrollValue.ServerScrollOptionBehaviour;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public final class RollerTerrainBehaviour extends ServerScrollOptionBehaviour<RollerTerrain> {
    public static final BehaviourType<RollerTerrainBehaviour> TYPE = new BehaviourType<>("trackwright_roller_terrain");
    public static final String KEY = "TrackwrightTerrain";
    public static final int NET_ID = 7477;

    public RollerTerrainBehaviour(SmartBlockEntity blockEntity) {
        super(RollerTerrain.class, blockEntity);
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }

    @Override
    public int netId() {
        return NET_ID;
    }

    @Override
    public void write(ValueOutput output, boolean clientPacket) {
        output.putInt(KEY, value);
    }

    @Override
    public void read(ValueInput input, boolean clientPacket) {
        value = clamp(input.getIntOr(KEY, 0));
    }

    public static RollerTerrain of(@Nullable CompoundTag blockEntityData) {
        return RollerTerrain.values()[clamp(blockEntityData == null ? 0 : blockEntityData.getIntOr(KEY, 0))];
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(RollerTerrain.values().length - 1, value));
    }
}
