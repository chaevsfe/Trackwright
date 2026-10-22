package dev.chaevsfe.trackwright.track;

import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;

import java.util.List;

public interface PlacementInfoExtension {
    boolean trackwright$bridged();

    void trackwright$setBridged(boolean bridged);

    List<BlockPos> trackwright$bridgedCells();

    void trackwright$setBridgedCells(List<BlockPos> cells);

    boolean trackwright$guessed();

    void trackwright$setGuessed(boolean guessed);

    @Nullable BorePlan trackwright$bore();

    void trackwright$setBore(@Nullable BorePlan bore);

    BoreAccess trackwright$boreAccess();

    void trackwright$setBoreAccess(BoreAccess access);

    boolean trackwright$converts();

    void trackwright$setConverts(boolean converts);

    @Nullable Gap trackwright$gap();

    void trackwright$setGap(@Nullable Gap gap);
}
