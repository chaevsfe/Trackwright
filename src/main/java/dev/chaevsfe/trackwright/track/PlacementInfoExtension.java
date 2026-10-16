package dev.chaevsfe.trackwright.track;

import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;

import java.util.List;

public interface PlacementInfoExtension {
    @Nullable String trackwright$refusal();

    void trackwright$setRefusal(@Nullable String refusal);

    boolean trackwright$bridged();

    void trackwright$setBridged(boolean bridged);

    List<BlockPos> trackwright$bridgedCells();

    void trackwright$setBridgedCells(List<BlockPos> cells);
}
