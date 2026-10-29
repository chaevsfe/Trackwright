package dev.chaevsfe.trackwright.actors;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import dev.chaevsfe.trackwright.TagReads;
import dev.chaevsfe.trackwright.config.RemoteSettings;
import dev.chaevsfe.trackwright.config.ServerConfig;
import dev.chaevsfe.trackwright.config.TrackwrightSettings;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

public final class RollerTerrainBehaviour extends ScrollOptionBehaviour<RollerTerrain> {
    public static final BehaviourType<RollerTerrainBehaviour> TYPE = new BehaviourType<>("trackwright_roller_terrain");
    public static final String KEY = "TrackwrightTerrain";
    public static final int NET_ID = 7477;

    private static volatile Supplier<ValueBoxTransform> slots = CenteredSideValueBoxTransform::new;

    public RollerTerrainBehaviour(SmartBlockEntity blockEntity) {
        super(RollerTerrain.class, Component.translatable("trackwright.roller.terrain"), blockEntity, slots.get());
        onlyActiveWhen(this::available);
    }

    public static void slots(Supplier<ValueBoxTransform> supplier) {
        slots = supplier;
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
    public void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        nbt.putInt(KEY, value);
    }

    @Override
    public void read(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        value = clamp(TagReads.intOr(nbt, KEY, 0));
    }

    private boolean available() {
        Level level = blockEntity.getLevel();
        if (level == null || !level.isClientSide()) {
            return ServerConfig.get().rollerReshape();
        }
        TrackwrightSettings settings = RemoteSettings.get();
        return settings != null && settings.rollerReshape();
    }

    public static RollerTerrain of(@Nullable CompoundTag blockEntityData) {
        return RollerTerrain.values()[clamp(blockEntityData == null ? 0 : TagReads.intOr(blockEntityData, KEY, 0))];
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(RollerTerrain.values().length - 1, value));
    }
}
