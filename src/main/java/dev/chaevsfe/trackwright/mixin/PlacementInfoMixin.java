package dev.chaevsfe.trackwright.mixin;

import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackPlacement;
import dev.chaevsfe.trackwright.track.BoreAccess;
import dev.chaevsfe.trackwright.track.BorePlan;
import dev.chaevsfe.trackwright.track.Gap;
import dev.chaevsfe.trackwright.track.PlacementInfoExtension;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

@Mixin(TrackPlacement.PlacementInfo.class)
public abstract class PlacementInfoMixin implements PlacementInfoExtension {
    @Shadow
    BezierConnection curve;
    @Shadow
    boolean valid;
    @Shadow
    int end1Extent;
    @Shadow
    int end2Extent;
    @Shadow
    String message;
    @Shadow
    Vec3 axis1;
    @Shadow
    Vec3 axis2;
    @Unique
    private boolean trackwright$bridged;
    @Unique
    private List<BlockPos> trackwright$bridgedCells = List.of();
    @Unique
    private boolean trackwright$guessed;
    @Unique
    private @Nullable BorePlan trackwright$bore;
    @Unique
    private BoreAccess trackwright$boreAccess = BoreAccess.NONE;
    @Unique
    private boolean trackwright$converts;
    @Unique
    private @Nullable Gap trackwright$gap;

    @Override
    public @Nullable BezierConnection trackwright$curve() {
        return curve;
    }

    @Override
    public boolean trackwright$valid() {
        return valid;
    }

    @Override
    public void trackwright$setValid(boolean valid) {
        this.valid = valid;
    }

    @Override
    public int trackwright$end1Extent() {
        return end1Extent;
    }

    @Override
    public void trackwright$setEnd1Extent(int extent) {
        end1Extent = extent;
    }

    @Override
    public int trackwright$end2Extent() {
        return end2Extent;
    }

    @Override
    public void trackwright$setEnd2Extent(int extent) {
        end2Extent = extent;
    }

    @Override
    public @Nullable String trackwright$message() {
        return message;
    }

    @Override
    public @Nullable Vec3 trackwright$axis1() {
        return axis1;
    }

    @Override
    public @Nullable Vec3 trackwright$axis2() {
        return axis2;
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

    @Override
    public boolean trackwright$guessed() {
        return trackwright$guessed;
    }

    @Override
    public void trackwright$setGuessed(boolean guessed) {
        trackwright$guessed = guessed;
    }

    @Override
    public @Nullable BorePlan trackwright$bore() {
        return trackwright$bore;
    }

    @Override
    public void trackwright$setBore(@Nullable BorePlan bore) {
        trackwright$bore = bore;
    }

    @Override
    public BoreAccess trackwright$boreAccess() {
        return trackwright$boreAccess;
    }

    @Override
    public void trackwright$setBoreAccess(BoreAccess access) {
        trackwright$boreAccess = access;
    }

    @Override
    public boolean trackwright$converts() {
        return trackwright$converts;
    }

    @Override
    public void trackwright$setConverts(boolean converts) {
        trackwright$converts = converts;
    }

    @Override
    public @Nullable Gap trackwright$gap() {
        return trackwright$gap;
    }

    @Override
    public void trackwright$setGap(@Nullable Gap gap) {
        trackwright$gap = gap;
    }
}
