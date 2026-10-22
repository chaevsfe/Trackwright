package dev.chaevsfe.trackwright.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;

public interface PlacementInfoExtension {
    @Nullable BezierConnection trackwright$curve();

    boolean trackwright$valid();

    void trackwright$setValid(boolean valid);

    int trackwright$end1Extent();

    void trackwright$setEnd1Extent(int extent);

    int trackwright$end2Extent();

    void trackwright$setEnd2Extent(int extent);

    @Nullable String trackwright$message();

    @Nullable Vec3 trackwright$axis1();

    @Nullable Vec3 trackwright$axis2();

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
