package dev.chaevsfe.trackwright.track;

import net.minecraft.core.BlockPos;

import java.util.List;

public interface PlacementInfoExtension {
    boolean trackwright$bridged();

    void trackwright$setBridged(boolean bridged);

    List<BlockPos> trackwright$bridgedCells();

    void trackwright$setBridgedCells(List<BlockPos> cells);

    boolean trackwright$guessed();

    void trackwright$setGuessed(boolean guessed);
}
