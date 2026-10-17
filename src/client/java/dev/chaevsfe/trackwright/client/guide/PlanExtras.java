package dev.chaevsfe.trackwright.client.guide;

import com.zurrtum.create.content.trains.track.TrackPlacement;
import com.zurrtum.create.content.trains.track.TrackPlacement.PlacementInfo;
import dev.chaevsfe.trackwright.client.mixin.TrackPlacementAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public record PlanExtras(boolean maximised, boolean locked, @Nullable PlacementInfo other) {
    private static @Nullable Key lastKey;
    private static @Nullable PlanExtras last;

    private record Key(
        BlockPos target,
        BlockState state,
        ItemStack stack,
        boolean maximise,
        boolean locked,
        int angle,
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
                && valid == key.valid
                && Objects.equals(message, key.message)
                && end1Extent == key.end1Extent
                && end2Extent == key.end2Extent;
        }

        @Override
        public int hashCode() {
            return Objects.hash(target, System.identityHashCode(state), System.identityHashCode(stack), maximise, locked, angle, valid, message);
        }
    }

    public static void reset() {
        lastKey = null;
        last = null;
    }

    public static PlanExtras of(Level level, Player player, BlockPos target, BlockState state, ItemStack stack, boolean maximise, PlacementInfo info) {
        boolean locked = WidestTurnLock.locked();
        Key key = new Key(target.immutable(), state, stack, maximise, locked, TrackPlacementAccessor.trackwright$hoveringAngle(),
            info.valid, info.message, info.end1Extent, info.end2Extent);
        PlanExtras previous = last;
        if (previous != null && key.equals(lastKey)) {
            return previous;
        }
        PlanExtras extras = new PlanExtras(maximise, locked, otherShape(level, player, target, state, stack, maximise, info));
        lastKey = key;
        last = extras;
        return extras;
    }

    private static @Nullable PlacementInfo otherShape(Level level, Player player, BlockPos target, BlockState state, ItemStack stack, boolean maximise, PlacementInfo info) {
        if (info.curve == null || !GuideLines.geometryFinal(info)) {
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
        return shape != null
            && GuideLines.geometryFinal(shape)
            && (shape.end1Extent != info.end1Extent || shape.end2Extent != info.end2Extent);
    }
}
