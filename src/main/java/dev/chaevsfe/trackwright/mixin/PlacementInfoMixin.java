package dev.chaevsfe.trackwright.mixin;

import com.zurrtum.create.content.trains.track.TrackPlacement;
import dev.chaevsfe.trackwright.track.PlacementInfoExtension;
import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

@Mixin(TrackPlacement.PlacementInfo.class)
public abstract class PlacementInfoMixin implements PlacementInfoExtension {
    @Unique
    private @Nullable String trackwright$refusal;
    @Unique
    private boolean trackwright$bridged;
    @Unique
    private List<BlockPos> trackwright$bridgedCells = List.of();

    @Override
    public @Nullable String trackwright$refusal() {
        return trackwright$refusal;
    }

    @Override
    public void trackwright$setRefusal(@Nullable String refusal) {
        trackwright$refusal = refusal;
    }

    @Override
    public boolean trackwright$bridged() {
        return trackwright$bridged;
    }

    @Override
    public void trackwright$setBridged(boolean bridged) {
        trackwright$bridged = bridged;
    }

    @Override
    public List<BlockPos> trackwright$bridgedCells() {
        return trackwright$bridgedCells;
    }

    @Override
    public void trackwright$setBridgedCells(List<BlockPos> cells) {
        trackwright$bridgedCells = List.copyOf(cells);
    }
}
