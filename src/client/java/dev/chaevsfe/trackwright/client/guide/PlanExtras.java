package dev.chaevsfe.trackwright.client.guide;

import com.simibubi.create.content.trains.track.TrackPlacement.PlacementInfo;
import com.simibubi.create.content.trains.track.TrackPlacement;
import dev.chaevsfe.trackwright.client.mixin.TrackPlacementAccessor;
import dev.chaevsfe.trackwright.track.PlacementInfoExtension;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public record PlanExtras(boolean maximised, boolean locked, @Nullable PlacementInfo other, @Nullable HeightColumn column) {
    private static @Nullable Key lastKey;
    private static @Nullable PlanExtras last;

    private record Key(
        BlockPos target,
        BlockState state,
        ItemStack stack,
        boolean maximise,
        boolean locked,
        int angle,
        boolean deep,
        boolean valid,
        @Nullable String message,
        int end1Extent,
        int end2Extent
    ) {
        @Override
        public boolean equals(Object other) {
            return other instanceof Key key
                && target.equals(key.target)
                && state == key.state
                && stack == key.stack
                && maximise == key.maximise
                && locked == key.locked
                && angle == key.angle
                && deep == key.deep
                && valid == key.valid
                && Objects.equals(message, key.message)
                && end1Extent == key.end1Extent
                && end2Extent == key.end2Extent;
        }

        @Override
        public int hashCode() {
            return Objects.hash(target, System.identityHashCode(state), System.identityHashCode(stack), maximise, locked, angle, deep, valid, message);
        }
    }

    public static void reset() {
        lastKey = null;
        last = null;
    }

    public static PlanExtras of(Level level, Player player, BlockPos target, BlockState state, ItemStack stack, boolean maximise, PlacementInfo info) {
        boolean locked = WidestTurnLock.locked();
        boolean deep = AimHelp.deepChecksAllowed();
        PlacementInfoExtension extension = (PlacementInfoExtension) info;
        Key key = new Key(target.immutable(), state, stack, maximise, locked, TrackPlacementAccessor.trackwright$hoveringAngle(),
            deep, extension.trackwright$valid(), extension.trackwright$message(), extension.trackwright$end1Extent(), extension.trackwright$end2Extent());
        PlanExtras previous = last;
        if (previous != null && key.equals(lastKey)) {
            return previous;
        }
        PlanExtras extras = deep
            ? new PlanExtras(maximise, locked, otherShape(level, player, target, state, stack, maximise, info),
                HeightColumn.compute(level, player, target, stack, maximise, info))
            : new PlanExtras(maximise, locked, null, null);
        lastKey = key;
        last = extras;
        return extras;
    }

    private static @Nullable PlacementInfo otherShape(Level level, Player player, BlockPos target, BlockState state, ItemStack stack, boolean maximise, PlacementInfo info) {
        PlacementInfoExtension extension = (PlacementInfoExtension) info;
        if (extension.trackwright$curve() == null || !extension.trackwright$valid()) {
            return null;
        }
        PlacementCache saved = PlacementCache.save();
        try {
            return TrackPlacement.tryConnect(level, player, target, state, stack, false, !maximise);
        } finally {
            saved.restore();
        }
    }

    public boolean shapeDependsOnWidest(PlacementInfo info) {
        PlacementInfo shape = other;
        if (shape == null || !GuideLines.geometryFinal(shape)) {
            return false;
        }
        PlacementInfoExtension planned = (PlacementInfoExtension) info;
        PlacementInfoExtension widest = (PlacementInfoExtension) shape;
        return widest.trackwright$end1Extent() != planned.trackwright$end1Extent() || widest.trackwright$end2Extent() != planned.trackwright$end2Extent();
    }
}
